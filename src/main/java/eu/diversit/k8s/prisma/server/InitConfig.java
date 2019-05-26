package eu.diversit.k8s.prisma.server;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads secrets from files (by mapping a Secret to a volume)
 * and creates a prisma.config in a provided folder.
 *
 * This folder should be an 'emptyFolder' which is shared with the Prisma Server container.
 * And the Prisma Server container must be setup to read config from a file.
 *
 * Exit values:
 * - 0 = ok
 * - 1 = input arguments missing
 * - 2 = error occured
 * - 3 = input/output folder does not exist
 */
public class InitConfig {

    private static final PrintStream LOGGER = System.out;
    private static final String FILE_MANAGEMENT_API_SECRET = "managementApiSecret";
    private static final String FILE_API_PORT = "apiPort";
    private static final String FILE_DB_CONNECTOR = "connector";
    private static final String FILE_DB_HOST = "host";
    private static final String FILE_DB_PORT = "dbPort";
    private static final String FILE_DB_USER = "user";
    private static final String FILE_DB_PASSWORD = "password";
    private static final String FILE_DB_MIGRATIONS = "migrations";
    private static final int MAX_SECRETS = 7;
    private static final int EXPECTED_NR_OF_ARGUMENTS = 1;
    private static final int EXIT_INPUT_ARGUMENTS_MISSING = 1;
    private static final int FIRST = 0;
    private static final int EXIT_FOLDER_DOES_NOT_EXIST = 3;
    private static final int EXIT_ERROR_OCCURED = 2;

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {

        String prismaConfig = createConfigString(args);

        if (args.length == 2) {
            // output to file
            Path output = Paths.get(args[1]);
            Files.write(output, prismaConfig.getBytes("UTF-8"));
        } else {
            // output to console
            LOGGER.println(prismaConfig);
        }

    }

    public static String createConfigString(String[] args) {
        if (args.length == 0 || args.length > 2) {
            LOGGER.println("Usage:");
            LOGGER.println("> InitConfig <secretFolder> [<path-to>/prisma.config]");
            System.exit(EXIT_INPUT_ARGUMENTS_MISSING);
        }

        Path secretsFolder = Paths.get(args[FIRST]);
        if (!Files.exists(secretsFolder)) {
            LOGGER.println("Secrets folder " + secretsFolder.toAbsolutePath() + " does not exist");
            System.exit(EXIT_FOLDER_DOES_NOT_EXIST);
        }

        SecretsReader secretsReader = new SecretsReader(secretsFolder);

        Optional<String> managementApiSecret = secretsReader.readOptional(FILE_MANAGEMENT_API_SECRET);
        Optional<String> apiPort = secretsReader.readRequired(FILE_API_PORT);
        Optional<String> dbConnector = secretsReader.readRequired(FILE_DB_CONNECTOR);
        Optional<String> dbHost = secretsReader.readRequired(FILE_DB_HOST);
        Optional<String> dbPort = secretsReader.readRequired(FILE_DB_PORT);
        Optional<String> dbUser = secretsReader.readRequired(FILE_DB_USER);
        Optional<String> dbPassword = secretsReader.readRequired(FILE_DB_PASSWORD);
        Optional<String> dbMigrations = secretsReader.readOptional(FILE_DB_MIGRATIONS);

        // if any error, show them and exit
        if (secretsReader.hasErrors()) {
            LOGGER.println("Errors: " + secretsReader.getErrors().stream().collect(Collectors.joining()));
            System.exit(EXIT_ERROR_OCCURED);
        }

        return new StringBuilder()
            .append(secretValue(as("managementApiSecret: %s\n"), managementApiSecret))
            .append(secretValue(as("port: %s\n"), apiPort))
            .append("databases:\n")
            .append("  default:\n")
            .append(secretValue(as("    connector: %s\n"), dbConnector))
            .append(secretValue(as("    host: %s\n"), dbHost))
            .append(secretValue(as("    port: %s\n"), dbPort))
            .append(secretValue(as("    user: %s\n"), dbUser))
            .append(secretValue(as("    password: %s\n"), dbPassword))
            .append(secretValue(as("    migrations: %s\n"), dbMigrations))
            .toString();
    }

    private static String secretValue(Function<String, String> formatter, Optional<String> optStr) {
        return optStr.map(formatter).orElse("");
    }

    private static Function<String, String> as(String format) {
        return str -> String.format(format, str);
    }

    /**
     * Function to read a file
     */
    private static class SecretsReader {
        private final Path secretsFolder;
        private final List<String> errors = new ArrayList<>(MAX_SECRETS);

        SecretsReader(Path secretsFolder) {
            this.secretsFolder = secretsFolder;
        }

        private Optional<String> read(String fileName, Boolean required) {
            Path filePath = secretsFolder.resolve(fileName);
            if (Files.notExists(filePath)) {
//                LOGGER.debug("Path {} does not exist", filePath.toAbsolutePath());
                if (required) {
                    errors.add(String.format("File %s missing", fileName));
                }
                return Optional.empty();
            }

            try {
                return Optional.ofNullable(Jdk11Files.readString(filePath));
            } catch (IOException e) {
                LOGGER.println("Error reading file " + fileName);
                errors.add(String.format("Cannot read secret {}", fileName));
                return Optional.empty();
            }
        }

        public Optional<String> readRequired(String filename) {
            return read(filename, true);
        }

        public Optional<String> readOptional(String filename) {
            return read(filename, false);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Temporary own implementation of JDK 11's Files.readString
     * which does not exist < JDK 11.
     */
    private static class Jdk11Files {

        public static String readString(Path filePath) throws IOException, OutOfMemoryError, SecurityException {

            return Files.readAllLines(filePath)
                    .stream()
                    .collect(Collectors.joining());
        }
    }
}
