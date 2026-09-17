package java.io;
public class DataInputStream extends InputStream implements DataInput {
    public DataInputStream(InputStream in) {}
    public int available() throws IOException { return 0; }
    public void close() throws IOException {}
    public void mark(int readlimit) {}
    public boolean markSupported() { return false; }
    public int read() throws IOException { return 0; }
    public int read(byte[] b) throws IOException { return 0; }
    public int read(byte[] b, int off, int len) throws IOException { return 0; }
    public boolean readBoolean() throws IOException { return false; }
    public byte readByte() throws IOException { return 0; }
    public char readChar() throws IOException { return 0; }
    public double readDouble() throws IOException { return 0; }
    public float readFloat() throws IOException { return 0; }
    public void readFully(byte[] b) throws IOException {}
    public void readFully(byte[] b, int off, int len) throws IOException {}
    public int readInt() throws IOException { return 0; }
    public long readLong() throws IOException { return 0; }
    public short readShort() throws IOException { return 0; }
    public int readUnsignedByte() throws IOException { return 0; }
    public int readUnsignedShort() throws IOException { return 0; }
    public String readUTF() throws IOException { return null; }
    public static String readUTF(DataInput in) throws IOException { return null; }
    public void reset() throws IOException {}
    public long skip(long n) throws IOException { return 0; }
    public int skipBytes(int n) throws IOException { return 0; }
}
