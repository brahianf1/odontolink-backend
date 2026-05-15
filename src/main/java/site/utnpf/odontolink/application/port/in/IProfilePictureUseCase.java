package site.utnpf.odontolink.application.port.in;

/**
 * Puerto de entrada para la gestion de la foto de perfil del usuario
 * autenticado (RF06 - extension).
 *
 * <p>Se separa del {@link IProfileUseCase} general porque la subida multipart
 * tiene reglas propias (validacion de MIME, tamanio, redimensionado) y
 * dependencias distintas (object storage) que no aplican al resto del
 * autoservicio de perfil. Mantenerlos en interfaces separadas evita que el
 * mas amplio termine arrastrando dependencias de storage que solo necesita
 * un endpoint puntual.
 */
public interface IProfilePictureUseCase {

    /**
     * Procesa el archivo subido por el usuario:
     * <ol>
     *   <li>Valida que sea una imagen JPEG o PNG real (no por extension).</li>
     *   <li>Valida que no exceda el limite configurado.</li>
     *   <li>Aplica crop centrado y resize al tamanio target.</li>
     *   <li>Re-codifica como JPEG con la calidad configurada.</li>
     *   <li>Sube al object storage con una clave unica.</li>
     *   <li>Borra la foto anterior del storage si existia y era propia.</li>
     *   <li>Persiste la nueva URL publica en el perfil del usuario.</li>
     * </ol>
     *
     * @return URL publica de la imagen final.
     */
    String uploadProfilePicture(Long userId, byte[] content, String originalFilename);

    /**
     * Elimina la foto de perfil del usuario tanto del storage (si era propia)
     * como del campo {@code profilePictureUrl} en BD. Idempotente: si el
     * usuario no tiene foto, la operacion no produce error.
     */
    void deleteProfilePicture(Long userId);
}
