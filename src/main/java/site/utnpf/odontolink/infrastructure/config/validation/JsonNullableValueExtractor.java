package site.utnpf.odontolink.infrastructure.config.validation;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.UnwrapByDefault;
import jakarta.validation.valueextraction.ValueExtractor;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Permite que Bean Validation atraviese el wrapper {@link JsonNullable} y
 * aplique las restricciones declaradas sobre su tipo contenido. Sin este
 * extractor, las anotaciones puestas como type-use (p.ej.
 * {@code JsonNullable<@Size(max=20) String>}) serian ignoradas.
 *
 * Se registra como Service via META-INF/services para que Hibernate Validator
 * lo descubra automaticamente en el arranque.
 */
@UnwrapByDefault
public class JsonNullableValueExtractor implements ValueExtractor<JsonNullable<@ExtractedValue ?>> {

    @Override
    public void extractValues(JsonNullable<@ExtractedValue ?> originalValue,
                              ValueReceiver receiver) {
        // Solo si el campo viene presente en el JSON validamos su valor: si
        // es undefined, semanticamente significa "no tocar" y no procede validar.
        if (originalValue != null && originalValue.isPresent()) {
            receiver.value(null, originalValue.get());
        }
    }
}
