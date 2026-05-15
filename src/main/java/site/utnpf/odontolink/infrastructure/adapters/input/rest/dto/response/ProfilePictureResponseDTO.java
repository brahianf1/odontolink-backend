package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta del endpoint de subida de foto de perfil. Contiene la URL
 * publica final tras el procesamiento server-side (crop + resize + reencoding
 * a JPEG).
 */
@Schema(description = "Respuesta tras subir o sustituir la foto de perfil")
public class ProfilePictureResponseDTO {

    @Schema(description = "URL publica de la nueva foto de perfil",
            example = "https://pub-xxxx.r2.dev/profile-pictures/15/3f9a-...-2c.jpg")
    private String profilePictureUrl;

    public ProfilePictureResponseDTO() {
    }

    public ProfilePictureResponseDTO(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
