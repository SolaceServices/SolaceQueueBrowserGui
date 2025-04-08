package com.solace.psg.queueBrowser.gui;

import org.apache.commons.cli.*;

import com.solace.psg.brokers.BrokerException;

public class CommandLineParser {
	private static final String configFile = "config";
	public String configFileProvided = "";
	
    public void parseArgs(String[] args) throws BrokerException {
        Options options = new Options();
        Option configFileArg = Option.builder("c")
                                 .longOpt(configFile)
                                 .hasArg()
                                 .argName("CONFIG_FILE")
                                 .desc("Configuration file. A json file specifying the broker to poll and the ")
                                 .required(true)
                                 .build();
        options.addOption(configFileArg);
        DefaultParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            configFileProvided = cmd.getOptionValue(configFile);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ApacheCLIExample", options);
            throw new BrokerException(e);
        }
    }
}
