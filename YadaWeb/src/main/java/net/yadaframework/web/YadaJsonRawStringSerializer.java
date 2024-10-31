package net.yadaframework.web;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serialize a String to JSON as a raw value i.e. without double quotes and escaping.
 * Can be applied to an attribute with this annotation:
 * <pre>@JsonSerialize(using = YadaJsonRawStringSerializer.class)</pre>
 */
public class YadaJsonRawStringSerializer extends JsonSerializer<String> { 
    
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
