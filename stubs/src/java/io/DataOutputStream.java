package java.io;
public class DataOutputStream extends OutputStream implements DataOutput {
    public DataOutputStream(OutputStream out) {}
    public void close() throws IOException {}
    public void flush() throws IOException {}
    public void write(int b) throws IOException {}
    public void write(byte[] b) throws IOException {}
    public void write(byte[] b, int off, int len) throws IOException {}
    public void writeBoolean(boolean v) throws IOException {}
    public void writeByte(int v) throws IOException {}
    public void writeChar(int v) throws IOException {}
    public void writeChars(String s) throws IOException {}
    public void writeDouble(double v) throws IOException {}
    public void writeFloat(float v) throws IOException {}
    public void writeInt(int v) throws IOException {}
    public void writeLong(long v) throws IOException {}
    public void writeShort(int v) throws IOException {}
    public void writeUTF(String s) throws IOException {}
}
