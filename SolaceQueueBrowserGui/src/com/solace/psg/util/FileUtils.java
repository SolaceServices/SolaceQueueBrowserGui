package com.solace.psg.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
	public static String loadFile(String path) throws IOException {
		Path filePath =  Paths.get(path);
	    byte[] bytes = Files.readAllBytes(filePath);
	    return new String (bytes);
	}

}
