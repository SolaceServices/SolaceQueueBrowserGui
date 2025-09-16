package com.solace.psg.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandLog {
	private static CommandLog m_instance = null;
	private String file;
	public static CommandLog instance() {
		if (m_instance == null) {
			m_instance = new CommandLog("logs/command.log");
		}
		return m_instance;
	}
	public CommandLog(String file) {
		this.file = file;
	}
	public void log(String text) {
        // Format timestamp
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);

        // Final log entry
        String logEntry = "[" + timestamp + "] " + text + "\n";
		
        FileWriter writer = null;
        try {
    		writer = new FileWriter(file, true);
			writer.write(logEntry);
		} catch (IOException e) {
			e.printStackTrace();
		}
        finally {
        	if (writer != null) {
        		try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
	}
}
