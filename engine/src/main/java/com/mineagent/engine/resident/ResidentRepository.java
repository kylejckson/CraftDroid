package com.mineagent.engine.resident;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Atomic snapshots. Malformed saves fail visibly and are never overwritten by an empty import. */
public final class ResidentRepository {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    public ResidentRepository(Path file) { this.file=file; }
    public List<ResidentProfile> load() throws IOException {
        if (!Files.exists(file)) return List.of();
        if (Files.size(file)>16_777_216) throw new IOException("Resident snapshot exceeds 16 MiB");
        try {
            var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.get("version").getAsInt()!=1) throw new IOException("Unknown resident data version");
            var entries=JSON.fromJson(root.get("residents"),ResidentProfile[].class);
            var ids=new HashSet<UUID>();var names=new HashSet<String>();
            for(var p:entries) if(!ids.add(p.id())||!names.add(p.name().toLowerCase(Locale.ROOT)))
                throw new IOException("Duplicate resident identity or name");
            return List.of(entries);
        } catch (RuntimeException e) { throw new IOException("Invalid resident snapshot; original retained",e); }
    }
    public synchronized void save(Collection<ResidentProfile> records) throws IOException {
        Files.createDirectories(file.getParent());
        var root=new JsonObject();root.addProperty("version",1);root.add("residents",JSON.toJsonTree(records));
        Path temporary=file.resolveSibling(file.getFileName()+".tmp");
        Files.writeString(temporary,JSON.toJson(root));
        try { Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch(AtomicMoveNotSupportedException e) { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); }
    }
}
