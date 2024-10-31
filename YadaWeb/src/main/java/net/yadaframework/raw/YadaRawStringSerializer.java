package net.yadaframework.raw;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serialize a String to JSON as a raw value i.e. without double quotes and escaping.
 * Can be applied to an attribute with this annotation:
 * <pre>@JsonSerialize(using = YadaRawStringSerializer.class)</pre>
 */
public class YadaRawStringSerializer extends JsonSerializer<String> { 
    
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (value.isEmpty()) {
            gen.writeString("");
            return;
        }

        gen.writeRawValue(value);
    }
}
