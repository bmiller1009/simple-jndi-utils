package org.bradfordmiller.simplejndiutils;

import io.vavr.control.Either;
import org.apache.commons.io.FileUtils;
import org.osjava.sj.jndi.MemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.activation.UnknownObjectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import javax.naming.*;
import javax.sql.DataSource;
import java.util.function.BiConsumer;
import java.util.stream.*;

/***
 * Helper functions for the simple-jndi API
 * @author - Bradford Miller
 */
public class JNDIUtils {

    private static final Logger logger = LoggerFactory.getLogger(JNDIUtils.class);

    /***
     *
     * @param initCtx - context to extract memory context from
     * @param contextName - name of context being targeted
     * @return a nullable memory context based on {@code initCtx} and {@code contextName}
     */
    public static MemoryContext getMemoryContextFromInitContext(InitialContext initCtx, String contextName) {
        try {
            var mc = (MemoryContext) initCtx.lookup(contextName); // as MemoryContext);
            return mc;
        } catch (NamingException ne) {
            logger.error(String.format("Naming exception occurred on jndi lookup of context %s: %s", contextName, ne.getMessage()));
            return null;
        }
    }
    /***
     *
     * @param jndi - the jndi name being referenced
     * @param context - the context being targeted
     * @return an Either of a DataSource or a Map based on the {@code jndi} and {@code context}
     */
    public static Either<DataSource, Map<String, String>> getDataSource(String jndi, String context) {
        try {
            var ctx = (Context) new InitialContext();
            var mc = (MemoryContext) ctx.lookup(context);
            var lookup = mc.lookup(jndi);

            if (lookup instanceof DataSource) {
                return Either.left((DataSource)lookup);
            } else if (lookup instanceof Map) {
                return Either.right((Map<String, String>)lookup);
            } else {
                throw new UnknownObjectException(String.format("jndi entry %s for context %s is neither a DataSource or a Map<string, String", jndi, context));
            }
        } catch (NamingException | UnknownObjectException ne) {
            logger.error(String.format("Naming exception occurred on jndi lookup of context %s: %s", context, ne.getMessage()));
            return null;
        }
    }
    /***
     *
     * @param ds - the datasource from which a connection is created
     * @return a java.sql.Connection
     * @throws SQLException
     */
    public static Connection getConnection(DataSource ds) throws SQLException {
        try {
           return ds.getConnection();
        } catch (SQLException sqlEx) {
            logger.error(String.format("Error fetching connection from data source: %s", sqlEx.getMessage()));
            throw sqlEx;
        }
    }
    /***
     *
     * @param jndiString - jndi entry to be looked up
     * @param context - context to lookup jndi string in
     * @return a java.sql connection based on the {@code jndiString} and {@code context}
     * @throws SQLException
     */
    public static Connection getJndiConnection(String jndiString, String context) throws SQLException {
        var ds = (DataSource) Either.left(getDataSource(jndiString, context)).getLeft();
        return getConnection(ds);
    }
    /***
     *
     * @param context a context to find property files for, if null then it uses a new context
     * @return a list of property files which comprise the jndi contexts
     * @throws NamingException
     * @throws IOException
     */
    public static List<String> getAvailableJndiContexts(InitialContext context) throws NamingException, IOException {

        if(context == null)
            context = new InitialContext();

        var root = context.getEnvironment().get("org.osjava.sj.root").toString();
        try(var pathFiles = Files.walk(Paths.get(root))) {
            var files =
                    pathFiles.map(f ->
                            f.toString()).filter(f -> f.endsWith(".properties")).collect(Collectors.toList());
            return files;
        } catch (IOException ioex) {
            logger.error(String.format("Error accessing available jndi contexts: %s", ioex.getMessage()));
            throw ioex;
        }
    }
    /***
     *
     * @param memoryContext context to lookup entries for
     * @return a map of key value pairs from a specific {@code memoryContext}
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static Map<String, String> getEntriesForJndiContext(MemoryContext memoryContext) throws NoSuchFieldException, IllegalAccessException {

        var field = memoryContext.getClass().getDeclaredField("namesToObjects");
        field.setAccessible(true);

        var fieldMap = (Map<Name, Object>) field.get(memoryContext);
        var map =
                fieldMap.entrySet().stream().collect(Collectors.toMap(
                        k -> String.valueOf(k.getKey().toString()),
                        v -> String.valueOf(v.getValue()))
                );

        return map;
    }
    /***
     *
     * @param context context to look up entry
     * @param jndiName jndi to look up in context
     * @param entry entry within jndi to pull entry for
     * @return a map entry for a specific {@code context} and jndi {@code jndiName} and the jndi {@code entry}
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static Map.Entry<String, String> getDetailsforJndiEntry(InitialContext context, String jndiName, String entry) throws NoSuchFieldException, IllegalAccessException {
        var mc = getMemoryContextFromInitContext(context, jndiName);
        var entries = getEntriesForJndiContext(mc);
        return new AbstractMap.SimpleEntry<>(entry, entries.get(entry));
    }
    /***
     *
     * @param jndiName jndi to look up in context
     * @param context context to look up entry
     * @param values map of values to save to the jndi and context
     * @return indication that the new jndi connection was saved
     * @throws NamingException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public static Boolean addJndiConnection(String jndiName, String context, Map<String, String> values) throws NamingException, IOException, IllegalAccessException, NoSuchFieldException {

        BiConsumer<File, String> writeToFile = (backupFile, delimiter) -> {

            try (var bw = new BufferedWriter(new FileWriter(backupFile.getAbsolutePath(), true))) {
                bw.write("\n");
                values.forEach((k,v) ->
                        {
                            try {
                                bw.write(String.format("%s%s%s=%s\n", jndiName, delimiter, k, v));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
            } catch (IOException ioex) {
                logger.error(String.format("Error adding jndi connection: %s", ioex.getMessage()));
                throw new RuntimeException(ioex);
            }
        };

        var ctx = new InitialContext();

        var root = ctx.getEnvironment().get("org.osjava.sj.root").toString();
        var colonReplace = ctx.getEnvironment().get("org.osjava.sj.colon.replace");
        var delimiter = ctx.getEnvironment().get("org.osjava.sj.delimiter").toString();
        var file = new File(String.format("%s/%s.properties", root, context));
        var lockFile = new File(String.format("%s/.LOCK", root));

        try {

            if(lockFile.exists()) {
                logger.info("jndi director is currently being modified. Please try to add your new entry later.");
                return false;
            } else {
                logger.info("Lock file is absent. Locking directory before proceeding");
                lockFile.createNewFile();

                var memoryContext = getMemoryContextFromInitContext(ctx, context);

                if (memoryContext != null) {
                    //Check if the key being added already exists
                    var fieldKeys = getEntriesForJndiContext(memoryContext);

                    if (fieldKeys.containsKey(jndiName)) {
                        var errorString = String.format("Jndi name $jndiName already exists for context %s.", context);
                        logger.error(errorString);
                        throw new IllegalAccessException(errorString);
                    } else {
                        var uuid = UUID.randomUUID().toString();
                        var backupFile =
                                new File(file.getParentFile().getName() + "/" + context + "_" + uuid + ".properties");
                        //Append the data to the copy
                        FileUtils.copyFile(file, backupFile);
                        writeToFile.accept(backupFile, delimiter);
                        //Rename the copy to the original
                        backupFile.renameTo(file);
                        return true;
                    }

                } else {
                    var f = new File(String.format("%s/%s.properties", root, context));
                    //Write the data to the copy
                    writeToFile.accept(f, delimiter);
                    return true;
                }
            }
        } catch (IllegalAccessException iaex) {
            logger.error(String.format("Illegal access exception while adding jndi connection: %s", iaex.getMessage()));
            throw iaex;
        } catch (NoSuchFieldException nsfex) {
            logger.error(String.format("No such field exception hit while adding jndi connection: %s", nsfex.getMessage()));
            throw nsfex;
        } finally {
            logger.info("Deleting lock file. Directory is writable.");
            lockFile.delete();
        }
    }
}
