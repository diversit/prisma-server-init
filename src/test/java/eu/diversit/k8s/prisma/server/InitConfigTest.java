package eu.diversit.k8s.prisma.server;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitConfigTest {

    @Test
    public void createConfigStringAllOptions() {

        String configString = InitConfig.createConfigString(new String[]{"src/test/resources/secrets/all"});

        assertEquals("managementApiSecret: testManagementApiSecret\n" +
                "port: testApiPort\n" +
                "databases:\n" +
                "  default:\n" +
                "    connector: testConnector\n" +
                "    host: testHost\n" +
                "    port: testDbPort\n" +
                "    user: testuser\n" +
                "    password: testpassword\n" +
                "    migrations: testMigrations\n", configString);
    }

    @Test
    public void createConfigStringRequiredOnly() {

        String configString = InitConfig.createConfigString(new String[]{"src/test/resources/secrets/required-only"});

        assertEquals("port: testApiPort\n" +
                "databases:\n" +
                "  default:\n" +
                "    connector: testConnector\n" +
                "    host: testHost\n" +
                "    port: testDbPort\n" +
                "    user: testuser\n" +
                "    password: testpassword\n", configString);
    }

    @Test
    @ExpectSystemExitWithStatus(1)
    public void initConfigShouldFailWhenArgumentMissing() {

        InitConfig.createConfigString(new String[0]);
    }

    @Test
    @ExpectSystemExitWithStatus(3)
    public void initConfigShouldFailWhenSecretsFolderDoesNotExist() {

        InitConfig.createConfigString(new String[] {"not-existing-folder" });
    }
}