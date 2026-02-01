package fi.gekkio.ghidraboy;

import generic.hash.HashUtilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HashBridge {
    public String getSha256Hash(InputStream input) throws IOException {
        return HashUtilities.getHash(HashUtilities.SHA256_ALGORITHM, input);
    }

    public String getSha256Hash(ByteArrayInputStream input) throws IOException {
        return HashUtilities.getHash(HashUtilities.SHA256_ALGORITHM, input);
    }
}
