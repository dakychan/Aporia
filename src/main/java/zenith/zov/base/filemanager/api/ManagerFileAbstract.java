package zenith.zov.base.filemanager.api;

import com.google.gson.Gson;
import lombok.Getter;
import zenith.zov.Zenith;
import zenith.zov.utility.crypt.CryptUtility;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

public class ManagerFileAbstract<T> {

    @Getter
    private Collection<T> items;
    private final String fileName;
    private final String shifr;
    private final Type type;
    private final Supplier<Collection<T>> collectionSupplier;

    public ManagerFileAbstract(String fileName, String shifr, Type type, Supplier<Collection<T>> collectionSupplier) {
        this.fileName = fileName;
        this.shifr = shifr;
        this.type = type;
        this.collectionSupplier = collectionSupplier;

        File file = new File(Zenith.DIRECTORY, fileName);

        if (!file.exists()) {
            try {
                file.createNewFile();
                items = collectionSupplier.get();
            } catch (Exception ignored) {
                items = collectionSupplier.get();
            }
        } else {
            load();
        }
    }

    public void save() {
        Gson gson = new Gson();
        String json = gson.toJson(items);
        try (FileWriter writer = new FileWriter(new File(Zenith.DIRECTORY, fileName))) {
            writer.write(shifr.isEmpty()
                    ? json
                    : Base64.getEncoder().encodeToString(CryptUtility.encryptData(json.getBytes(), shifr)));
        } catch (Exception ignored) {
        }
    }

    public void load() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(Zenith.DIRECTORY, fileName)))) {
            Gson gson = new Gson();

            if (!shifr.isEmpty()) {
                String encryptedDataBase64 = reader.readLine();
                if (encryptedDataBase64 != null && !encryptedDataBase64.isEmpty()) {
                    byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
                    byte[] decryptedData = CryptUtility.decryptData(encryptedData, shifr);
                    String json = new String(decryptedData, StandardCharsets.UTF_8);
                    items = gson.fromJson(json, type);
                } else {
                    items = collectionSupplier.get();
                }
            } else {
                items = gson.fromJson(reader, type);
            }

            if (items == null) {
                items = collectionSupplier.get();
            }

        } catch (Exception ignored) {
            items = collectionSupplier.get();
        }
    }

    public void add(T item) {
        items.add(item);
    }

    public void remove(T item) {
        items.remove(item);
    }
}
