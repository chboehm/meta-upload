package de.idadachverband.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import lombok.Cleanup;

public class JsonHelper
{
    public static JsonObject loadJsonFile(Path path) throws IOException
    {
        @Cleanup 
        InputStream in = Files.newInputStream(path, StandardOpenOption.READ);
        @Cleanup
        JsonReader reader = Json.createReader(in);
        JsonObject object = reader.readObject();
        return object;
    }
    
    public static void storeJsonFile(JsonObjectBuilder object, Path path) throws IOException
    {
        Files.createDirectories(path.getParent());
        @Cleanup
        OutputStream out = Files.newOutputStream(path,  StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        @Cleanup
        JsonWriter writer = Json.createWriter(out);
        writer.writeObject(object.build());
    }
}
