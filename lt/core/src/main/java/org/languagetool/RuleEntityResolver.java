package org.languagetool;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.io.ByteStreams;
import org.languagetool.tools.StringTools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves an XML file's external entity URIs as relative paths.
 */
public class RuleEntityResolver implements EntityResolver {

  /**
   * Resolves the entity's absolute path by taking the relative path found in the source XML and combining it with the
   * path of the source XML file (which also must be resolved to a full absolute path).
   * @param publicId The public identifier of the external entity
   *        being referenced, or null if none was supplied.
   * @param systemId The system identifier of the external entity
   *        being referenced.
   * @return InputSource An InputSource constructed from the resolved path of the file the external entity points to
   */
  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    if (systemId != null && systemId.endsWith(".ent")) {
      return new InputSource(getInputStreamLTEntities(systemId));
    }
    return null;
  }

  public String getPathFromLTResourceFolder(String input) {
    // we assume that the entities file is in the resource folder of each language
    return input.replaceAll(".*/resource/", "").replaceAll("\\.\\./", "");
  }

  public InputStream getInputStreamLTEntities(String input) {
    return JLanguageTool.getDataBroker()
      .getFromResourceDirAsStream(getPathFromLTResourceFolder(input));
  }

  private static final Pattern ENTITY_FILE_DECL = Pattern.compile("<!ENTITY\\s+%\\s+(\\w+)\\s+SYSTEM\\s+\"([^\"]+\\.ent)\"\\s*>");

  /**
   * Android's expat parser cannot load external parameter entities and silently expands them to nothing,
   * so there the {@code .ent} files a rule file references are inlined into its DOCTYPE before parsing.
   */
  public static InputStream inlineEntityFilesOnAndroid(InputStream in) throws IOException {
    if (!StringTools.IS_ANDROID) {
      return in;
    }
    String xml = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
    Matcher m = ENTITY_FILE_DECL.matcher(xml);
    while (m.find()) {
      String entities;
      try (InputStream ent = new RuleEntityResolver().getInputStreamLTEntities(m.group(2))) {
        entities = new String(ByteStreams.toByteArray(ent), StandardCharsets.UTF_8);
      }
      String rest = xml.substring(m.end()).replace("%" + m.group(1) + ";", entities);
      xml = xml.substring(0, m.start()) + rest;
      m = ENTITY_FILE_DECL.matcher(xml);
    }
    return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
  }
}
