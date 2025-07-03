package ust.tad.terraformmpsplugin.analysis.terraformParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HCL2JSONExecutor {

    /**
     * Execute a hcl2json command on the local command line for converting Terraform files to the
     * JSON format.
     * The hcl2json tool is added to the project as a separate Git Submodule.
     *
     * @param fileName the file for which to execute the hcl2json conversion
     * @return the content of the Terraform file as a JSONNode.
     * @throws IOException          if an I/O error occurs while executing the command or reading
     *                              the JSON input stream.
     * @throws InterruptedException if the execution of the command is interrupted.
     */
    public JsonNode executeHCL2JSONCommand(String fileName) throws IOException,
            InterruptedException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", "hcl2json/hcl2json", fileName);
        } else {
            builder.command("./hcl2json/hcl2json", fileName);
        }
        Process process = builder.start();
        process.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return new ObjectMapper().readTree(reader);
    }

}
