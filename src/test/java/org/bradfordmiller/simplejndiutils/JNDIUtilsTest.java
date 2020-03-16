package org.bradfordmiller.simplejndiutils;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class JNDIUtilsTest {

    @BeforeAll
    public static void removeContext() throws NamingException {
        var ctx = "default_ds_3";
        var initCtx = new InitialContext();
        var root = initCtx.getEnvironment().get("org.osjava.sj.root").toString();
        var file = String.format("%s/%s.properties", root, ctx);
        if (new File(file).exists())
            new File(file).delete();
        initCtx.close();
    }
    @Test
    public void addNewJndiResources() throws NamingException, IllegalAccessException, NoSuchFieldException, IOException {

        var initialContext = new InitialContext();
        var jndi = "BradTestJNDI";
        var ctx = "default_ds_3";
        var root = initialContext.getEnvironment().get("org.osjava.sj.root").toString();

        JNDIUtils.addJndiConnection(
                jndi,
                ctx,
                Map.of(
                        "type","java.util.Map",
                        "targetName","src/test/resources/data/outputData/ds3"
                )
        );

        JNDIUtils.addJndiConnection(
                "BradTestJNDI_22",
                ctx,
                Map.of(
                        "type", "java.util.Map",
                        "targetName","src/test/resources/data/outputData/testJNDI"
                )
        );

        JNDIUtils.addJndiConnection(
                "BradTestJNDI_23",
                ctx,
                Map.of(
                        "type","javax.sql.DataSource",
                        "driver","org.sqlite.JDBC",
                        "url","jdbc:sqlite:src/test/resources/data/outputData/real_estate.db",
                        "user" ,"test_user",
                        "password" ,"test_password"
                )
        );

        var entryAddNew = JNDIUtils.getDataSource(jndi, ctx);
        var entryAddExistingCsv = JNDIUtils.getDataSource("BradTestJNDI_22", ctx);
        var entryAddExisingSql = JNDIUtils.getDataSource("BradTestJNDI_23", ctx);

        assert(entryAddNew instanceof Either.Right);
        assert(entryAddExistingCsv instanceof Either.Right);
        assert(entryAddExisingSql instanceof Either.Left);

        new File(String.format("%s/%s.properties", root, ctx)).delete();
    }
    @Test
    public void getAllJndiContexts() throws IOException, NamingException {
        var contexts = JNDIUtils.getAvailableJndiContexts(null);
        assert(contexts.size() == 1);
        assert(contexts.get(0).equals("src/test/resources/jndi/default_ds.properties"));
    }
    @Test
    public void getEntriesForJndiContext() throws NoSuchFieldException, IllegalAccessException, NamingException {
        var initCtx = new InitialContext();
        var contextName = "default_ds";
        var mc = JNDIUtils.getMemoryContextFromInitContext(initCtx, contextName);
        var entries = JNDIUtils.getEntriesForJndiContext(mc);

        assert(mc != null);
        assert(entries.size() == 2);
        assert(entries.get("RealEstateOutDupesUseDefaults").equals("{targetName=src/test/resources/data/outputData/dupeName}"));
    }
    @Test
    public void getJndiEntry() throws NamingException, NoSuchFieldException, IllegalAccessException {
        var initCtx = new InitialContext();
        var contextName = "default_ds";
        var entry = "SqlLiteTestPW";
        var entryResult = JNDIUtils.getDetailsforJndiEntry(initCtx, contextName, entry);

        assert(entryResult.getKey().equals("SqlLiteTestPW"));
        assert(entryResult.getValue().equals("org.sqlite.JDBC::::jdbc:sqlite:src/test/resources/data/outputData/real_estate.db::::TestUser"));
    }
}