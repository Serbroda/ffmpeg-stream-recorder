package de.serbroda;

public class FFmpegProcessOptions {

    private String workDirectory;
    private boolean printMessages;

    public String getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
    }

    public boolean isPrintMessages() {
        return printMessages;
    }

    public void setPrintMessages(boolean printMessages) {
        this.printMessages = printMessages;
    }
}
