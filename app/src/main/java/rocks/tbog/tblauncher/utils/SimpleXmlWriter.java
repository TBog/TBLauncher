package rocks.tbog.tblauncher.utils;

import android.util.Xml;

import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Wrapper class over most probably com.android.org.kxml2.io.KXmlSerializer
 * Because it's a platform dependent class we can't extend.
 */
public class SimpleXmlWriter implements XmlSerializer {
    private final XmlSerializer xmlSerializer;
    private String namespace = null;

    private SimpleXmlWriter(XmlSerializer serializer) {
        xmlSerializer = serializer;
    }

    public static SimpleXmlWriter getNewInstance() {
        /*
         * Returns a new instance of the platform default {@link XmlSerializer} more efficiently than
         * using {@code XmlPullParserFactory.newInstance().newSerializer()}.
         */
        XmlSerializer serializer = Xml.newSerializer();

        return new SimpleXmlWriter(serializer);
    }

    public boolean setIndentation(boolean turnOn, @Nullable String indentString) {
        try {
            xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", turnOn);
            if (indentString != null)
                xmlSerializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", indentString);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean setIndentation(boolean turnOn) {
        return setIndentation(turnOn, null);
    }

    /**
     * Most probably not supported
     *
     * @param separator line separator
     * @return true if serializer property exists
     */
    public boolean setLineSeparator(String separator) {
        try {
            xmlSerializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", separator);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void setCurrentNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public void setFeature(String name, boolean state) throws IllegalArgumentException, IllegalStateException {
        xmlSerializer.setFeature(name, state);
    }

    @Override
    public boolean getFeature(String name) {
        return xmlSerializer.getFeature(name);
    }

    @Override
    public void setProperty(String name, Object value) throws IllegalArgumentException, IllegalStateException {
        xmlSerializer.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) {
        return xmlSerializer.getProperty(name);
    }

    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.setOutput(os, encoding);
    }

    @Override
    public void setOutput(Writer writer) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.setOutput(writer);
    }

    @Override
    public void startDocument(String encoding, Boolean standalone) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.startDocument(encoding, standalone);
    }

    public void startDocument() throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.startDocument(StandardCharsets.UTF_8.name(), true);
    }

    @Override
    public void endDocument() throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.endDocument();
    }

    @Override
    public void setPrefix(String prefix, String namespace) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.setPrefix(prefix, namespace);
    }

    @Override
    public String getPrefix(String namespace, boolean generatePrefix) throws IllegalArgumentException {
        return xmlSerializer.getPrefix(namespace, generatePrefix);
    }

    @Override
    public int getDepth() {
        return xmlSerializer.getDepth();
    }

    @Override
    public String getNamespace() {
        return xmlSerializer.getNamespace();
    }

    @Override
    public String getName() {
        return xmlSerializer.getName();
    }

    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException, IllegalArgumentException, IllegalStateException {
        return xmlSerializer.startTag(namespace, name);
    }

    public SimpleXmlWriter startTag(String name) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.startTag(namespace, name);
        return this;
    }

    @Override
    public XmlSerializer attribute(String namespace, String name, String value) throws IOException, IllegalArgumentException, IllegalStateException {
        return xmlSerializer.attribute(namespace, name, value);
    }

    public SimpleXmlWriter attribute(String name, String value) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.attribute(namespace, name, value);
        return this;
    }

    public SimpleXmlWriter attribute(String name, int value) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.attribute(namespace, name, Integer.toString(value));
        return this;
    }

    @Override
    public XmlSerializer endTag(String namespace, String name) throws IOException, IllegalArgumentException, IllegalStateException {
        return xmlSerializer.endTag(namespace, name);
    }

    public SimpleXmlWriter endTag(String name) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.endTag(namespace, name);
        return this;
    }

    @Override
    public XmlSerializer text(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        return xmlSerializer.text(text);
    }

    public SimpleXmlWriter content(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.text(text);
        return this;
    }

    public SimpleXmlWriter content(int amount) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.text(String.valueOf(amount));
        return this;
    }

    @Override
    public XmlSerializer text(char[] buf, int start, int len) throws IOException, IllegalArgumentException, IllegalStateException {
        return xmlSerializer.text(buf, start, len);
    }

    public SimpleXmlWriter content(byte[] buf) throws IOException, IllegalArgumentException, IllegalStateException {
        final int size = buf.length;
        char[] text = new char[size];
        for (int i = 0; i < size; i += 1)
            text[i] = (char) buf[i];
        xmlSerializer.text(text, 0, size);
        return this;
    }

    /**
     * Wrap text in <![CDATA[ ... ]]> tags and make sure we have valid chars
     * boolean valid = (ch >= 0x20 && ch <= 0xd7ff) ||
     * (ch == '\t' || ch == '\n' || ch == '\r') ||
     * (ch >= 0xe000 && ch <= 0xfffd);
     *
     * @param text to be converted to char[] by calling toCharArray()
     * @throws IOException              from writer
     * @throws IllegalArgumentException when an invalid char is found or from writer
     * @throws IllegalStateException    from writer
     */
    @Override
    public void cdsect(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.cdsect(text);
    }

    @Override
    public void entityRef(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.entityRef(text);
    }

    @Override
    public void processingInstruction(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.processingInstruction(text);
    }

    @Override
    public void comment(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.comment(text);
    }

    /**
     * Write <!DOCTYPE text >
     *
     * @param text the text inside DOCTYPE
     * @throws IOException              from writer
     * @throws IllegalArgumentException from writer
     * @throws IllegalStateException    from writer
     */
    @Override
    public void docdecl(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.docdecl(text);
    }

    @Override
    public void ignorableWhitespace(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        xmlSerializer.ignorableWhitespace(text);
    }

    @Override
    public void flush() throws IOException {
        xmlSerializer.flush();
    }
}
