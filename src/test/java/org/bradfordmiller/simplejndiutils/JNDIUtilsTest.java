package org.bradfordmiller.simplejndiutils;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.osjava.sj.jndi.MemoryContext;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JNDIUtilsTest {

    @BeforeAll
    public static void removeContext() throws NamingException {
        String ctx = "default_ds_3";
        InitialContext initCtx = new InitialContext();
        String root = initCtx.getEnvironment().get("org.osjava.sj.root").toString();
        String file = String.format("%s/%s.properties", root, ctx);
        if (new File(file).exists())
            new File(file).delete();
        initCtx.close();
    }
    @Test
    public void addNewJndiResources() throws NamingException, IllegalAccessException, NoSuchFieldException, IOException {

        InitialContext initialContext = new InitialContext();
        String jndi = "BradTestJNDI";
        String ctx = "default_ds_3";
        String root = initialContext.getEnvironment().get("org.osjava.sj.root").toString();
        Map<String, String> jndiInfoMap = new HashMap<>();
        jndiInfoMap.put("type","java.util.Map");
        jndiInfoMap.put("targetName","src/test/resources/data/outputData/ds3");

        Map<String, String> jndiInfoTestJndi = new HashMap<>();
        jndiInfoTestJndi.put("type", "java.util.Map");
        jndiInfoTestJndi.put("targetName","src/test/resources/data/outputData/testJNDI");

        Map<String, String> jndiInfoJDBC= new HashMap<>();
        jndiInfoJDBC.put("type","javax.sql.DataSource");
        jndiInfoJDBC.put("driver","org.sqlite.JDBC");
        jndiInfoJDBC.put("url","jdbc:sqlite:src/test/resources/data/outputData/real_estate.db");
        jndiInfoJDBC.put("user" ,"test_user");
        jndiInfoJDBC.put("password" ,"test_password");

        JNDIUtils.addJndiConnection(jndi, ctx, jndiInfoMap);
        JNDIUtils.addJndiConnection("BradTestJNDI_22", ctx, jndiInfoTestJndi);
        JNDIUtils.addJndiConnection("BradTestJNDI_23", ctx, jndiInfoJDBC);

        Object entryAddNew = JNDIUtils.getDataSource(jndi, ctx);
        Object entryAddExistingCsv = JNDIUtils.getDataSource("BradTestJNDI_22", ctx);
        Object entryAddExisingSql = JNDIUtils.getDataSource("BradTestJNDI_23", ctx);

        assert(entryAddNew instanceof Either.Right);
        assert(entryAddExistingCsv instanceof Either.Right);
        assert(entryAddExisingSql instanceof Either.Left);

        new File(String.format("%s/%s.properties", root, ctx)).delete();
    }
    @Test
    public void getAllJndiContexts() throws IOException, NamingException {
        List<String> contexts = JNDIUtils.getAvailableJndiContexts(null);
        assert(contexts.size() == 1);
        assert(contexts.get(0).equals("src/test/resources/jndi/default_ds.properties"));
    }
    @Test
    public void getEntriesForJndiContext() throws NoSuchFieldException, IllegalAccessException, NamingException {
        InitialContext initCtx = new InitialContext();
        String contextName = "default_ds";
        MemoryContext mc = JNDIUtils.getMemoryContextFromInitContext(initCtx, contextName);
        Map<String, String> entries = JNDIUtils.getEntriesForJndiContext(mc);

        assert(mc != null);
        assert(entries.size() == 2);
        assert(entries.get("RealEstateOutDupesUseDefaults").equals("{targetName=src/test/resources/data/outputData/dupeName}"));
    }
    @Test
    public void getJndiEntry() throws NamingException, NoSuchFieldException, IllegalAccessException {
        InitialContext initCtx = new InitialContext();
        String contextName = "default_ds";
        String entry = "SqlLiteTestPW";
        Map.Entry<String, String> entryResult = JNDIUtils.getDetailsforJndiEntry(initCtx, contextName, entry);

        assert(entryResult.getKey().equals("SqlLiteTestPW"));
        assert(entryResult.getValue().equals("org.sqlite.JDBC::::jdbc:sqlite:src/test/resources/data/outputData/real_estate.db::::TestUser"));
    }
}