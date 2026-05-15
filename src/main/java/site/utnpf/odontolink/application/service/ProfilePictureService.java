package site.utnpf.odontolink.application.service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IProfilePictureUseCase;
import site.utnpf.odontolink.application.port.out.IObjectStoragePort;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Use case de gestion de foto de perfil (subida y borrado).
 *
 * <p>Validacion en capas defensivas, en este orden:
 * <ol>
 *   <li>Tamanio efectivo del binario (rechaza payloads > maxBytes).</li>
 *   <li>Detection del formato por contenido real con ImageIO (rechaza
 *       archivos que no son imagenes o que son formatos no permitidos).</li>
 *   <li>Decoding de la imagen (rechaza imagenes corruptas).</li>
 * </ol>
 *
 * <p>Tras validar, normaliza la imagen aplicando crop centrado al cuadrado
 * mayor inscrito y resize al tamanio target. El output siempre se reencoda
 * como JPEG para homogeneizar el tamanio y simplificar el contrato de la URL
 * publica. La clave generada incluye el {@code userId} y un UUID para evitar
 * cacheado agresivo de avatares anteriores.
 */
@Transactional
public class ProfilePictureService implements IProfilePictureUseCase {

    /**
     * Formatos de entrada aceptados. ImageIO los detecta automaticamente con
     * estos identificadores (lowercase) cuando inspecciona el contenido real.
     * WebP requiere un plugin externo (TwelveMonkeys); se omite hasta que sea
     * un requisito explicito.
     */
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "jpg", "png");

    private static final String KEY_PREFIX = "profile-pictures";

    private final UserRepository userRepository;
    private final IObjectStoragePort objectStorage;
    private final int maxBytes;
    private final int targetSizePx;
    private final double jpegQuality;

    public ProfilePictureService(UserRepository userRepository,
                                 IObjectStoragePort objectStorage,
                                 int maxBytes,
                                 int targetSizePx,
                                 double jpegQuality) {
        this.userRepository = userRepository;
        this.objectStorage = objectStorage;
        this.maxBytes = maxBytes;
        this.targetSizePx = targetSizePx;
        this.jpegQuality = jpegQuality;
    }

    @Override
    public String uploadProfilePicture(Long userId, byte[] content, String originalFilename) {
        validateSize(content);
        String inputFormat = detectImageFormat(content);

        byte[] normalized = normalizeToSquareJpeg(content);

        User user = loadUser(userId);
        String previousUrl = user.getProfilePictureUrl();

        String newKey = buildKey(userId);
        String newUrl = objectStorage.upload(newKey, normalized, "image/jpeg");

        user.setProfilePictureUrl(newUrl);
        userRepository.save(user);

        // El delete va al final porque preferimos quedar con dos copias (la
        // vieja huerfana y la nueva valida) ante un fallo intermedio, antes
        // que con cero copias por haber borrado primero.
        deletePreviousIfOwned(previousUrl);

        // El parametro permanece en la firma para diagnostico/log futuro: el
        // nombre de archivo del cliente NO se usa para construir la key
        // (puede contener path traversal o caracteres invalidos).
        if (originalFilename != null && !originalFilename.isBlank()) {
            // no-op de momento; gancho para auditoria
        }
        return newUrl;
    }

    @Override
    public void deleteProfilePicture(Long userId) {
        User user = loadUser(userId);
        String previousUrl = user.getProfilePictureUrl();
        if (previousUrl == null || previousUrl.isBlank()) {
            return;
        }
        user.setProfilePictureUrl(null);
        userRepository.save(user);
        deletePreviousIfOwned(previousUrl);
    }

    /**
     * Si la URL guardada pertenece a NUESTRO storage (empieza con la base
     * publica configurada), extraemos la key y borramos el objeto. Si la URL
     * apunta a un tercero (caso de cuentas antiguas que tenian una URL
     * externa cargada manualmente), la dejamos intacta para no afectar
     * recursos ajenos.
     */
    private void deletePreviousIfOwned(String previousUrl) {
        if (previousUrl == null || previousUrl.isBlank()) {
            return;
        }
        try {
            String expectedPrefix = objectStorage.buildPublicUrl("");
            if (previousUrl.startsWith(expectedPrefix)) {
                String previousKey = previousUrl.substring(expectedPrefix.length());
                objectStorage.delete(previousKey);
            }
        } catch (RuntimeException ex) {
            // Tratamos el fallo de borrado como recuperable: la nueva URL ya
            // esta persistida y el avatar anterior queda como objeto huerfano,
            // problema que un job de cleanup puede recoger mas tarde sin
            // romper la UX del usuario.
        }
    }

    private void validateSize(byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidBusinessRuleException("El archivo de imagen esta vacio.");
        }
        if (content.length > maxBytes) {
            throw new InvalidBusinessRuleException(
                    "El archivo supera el tamanio maximo permitido ("
                            + (maxBytes / 1024) + " KB).");
        }
    }

    private String detectImageFormat(byte[] content) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new InvalidBusinessRuleException(
                        "El archivo no es una imagen valida. Formatos aceptados: JPEG, PNG.");
            }
            String format = readers.next().getFormatName().toLowerCase();
            if (!ALLOWED_FORMATS.contains(format)) {
                throw new InvalidBusinessRuleException(
                        "Formato '" + format + "' no permitido. Use JPEG o PNG.");
            }
            return format;
        } catch (IOException ex) {
            throw new InvalidBusinessRuleException(
                    "No se pudo leer el archivo de imagen: " + ex.getMessage());
        }
    }

    /**
     * Aplica crop centrado al cuadrado mas grande inscrito en la imagen,
     * resize a {@code targetSizePx} y reencoda como JPEG con la calidad
     * configurada. Thumbnailator usa internamente algoritmos de muestreo
     * decentes (bicubic + Lanczos para downscale grande), lo que ahorra
     * implementar un pipeline propio.
     */
    private byte[] normalizeToSquareJpeg(byte[] content) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(content);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            BufferedImage source = ImageIO.read(in);
            if (source == null) {
                throw new InvalidBusinessRuleException("La imagen no se pudo decodificar.");
            }

            Thumbnails.of(source)
                    .size(targetSizePx, targetSizePx)
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(jpegQuality)
                    .toOutputStream(out);

            return out.toByteArray();
        } catch (IOException ex) {
            throw new InvalidBusinessRuleException(
                    "No se pudo procesar la imagen: " + ex.getMessage());
        }
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + "/" + userId + "/" + UUID.randomUUID() + ".jpg";
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", String.valueOf(userId)));
    }
}
