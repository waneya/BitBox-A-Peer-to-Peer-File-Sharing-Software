package unimelb.bitbox.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.spec.RSAPublicKeySpec;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Splitter;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.base.Charsets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;
import static com.google.common.io.BaseEncoding.base64;

/**
 * This class is adapted from stackoverflow's link below:
 * https://stackoverflow.com/a/51048024/10936659
 * @author Tarek Elbeik
 *
 */
public class OpenSSHToRSAPubKeyConverter {

  private static final String SSH_MARKER = "ssh-rsa";

  private ByteSource supplier;

  public OpenSSHToRSAPubKeyConverter(String fileName) {
    this(new File(fileName));
  }

  public OpenSSHToRSAPubKeyConverter(File file) {
    try {
      byte[] data = IOUtils.toByteArray(new FileInputStream(file));
      this.supplier = ByteSource.wrap(data);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public OpenSSHToRSAPubKeyConverter(byte[] data) {
    this.supplier = ByteSource.wrap(data);
  }

  /**
   * Converts an SSH public key to a x.509 compliant format RSA public key spec
   * Source: https://github.com/jclouds/jclouds/blob/master/compute/src/main/java/org/jclouds/ssh/SshKeys.java
   * @return RSAPublicKeySpec
   */
  public RSAPublicKeySpec convertToRSAPublicKey() {
    try {
      InputStream stream = supplier.openStream();
      
      String x = IOUtils.toString(stream, "UTF-8");
      Iterable<String> parts = Splitter.on(' ').split(x);
      checkArgument(size(parts) >= 2 && SSH_MARKER.equals(get(parts,0)), "bad format, should be: ssh-rsa AAAB3....");
      stream = new ByteArrayInputStream(base64().decode(get(parts, 1)));
      String marker = new String(readLengthFirst(stream));
      checkArgument(SSH_MARKER.equals(marker), "looking for marker %s but received %s", SSH_MARKER, marker);
      BigInteger publicExponent = new BigInteger(readLengthFirst(stream));
      BigInteger modulus = new BigInteger(readLengthFirst(stream));
      RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
      return keySpec;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static byte[] readLengthFirst(InputStream in) throws IOException {
    int[] bytes = new int[]{ in.read(), in.read(), in.read(), in.read() };
    int length = 0;
    int shift = 24;
    for (int i = 0; i < bytes.length; i++) {
      length += bytes[i] << shift;
      shift -= 8;
    }
    byte[] val = new byte[length];
    ByteStreams.readFully(in, val);
    return val;
  }
}