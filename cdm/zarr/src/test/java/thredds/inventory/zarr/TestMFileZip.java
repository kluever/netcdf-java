package thredds.inventory.zarr;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class TestMFileZip {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @RunWith(Parameterized.class)
  public static class TestMFileZipParameterized {

    @Parameterized.Parameters(name = "{0}, {1}")
    public static List<Integer[]> getTestParameters() {
      List<Integer[]> result = new ArrayList<>();
      result.add(new Integer[] {0, 0});
      result.add(new Integer[] {0, 1});
      result.add(new Integer[] {1, 1});
      result.add(new Integer[] {1000, 3});
      return result;
    }

    @Parameterized.Parameter(0)
    public int entrySize;

    @Parameterized.Parameter(1)
    public int numberOfEntries;

    @Test
    public void shouldWriteZipToStream() throws IOException {
      try (ZipFile zipFile = createTemporaryZipFile("TestWriteZip", entrySize, numberOfEntries)) {
        final MFileZip mFile = new MFileZip(zipFile.getName());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mFile.writeToStream(outputStream);
        assertThat(outputStream.size()).isEqualTo(mFile.getLength());
      }
    }

    @Test
    public void shouldWritePartialZipToStream() throws IOException {
      try (ZipFile zipFile = createTemporaryZipFile("TestWritePartialZip", entrySize, numberOfEntries)) {
        final MFileZip mFile = new MFileZip(zipFile.getName());
        final int length = (int) mFile.getLength();

        final int offset = 1;
        final int maxBytes = 100;

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final int startPosition = Math.min(offset, length);
        final int endPosition = Math.min(offset + maxBytes, length);

        mFile.writeToStream(outputStream, offset, maxBytes);
        assertThat(outputStream.size()).isEqualTo(Math.max(0, endPosition - startPosition));
      }
    }
  }

  public static class TestMFileZipNonParameterized {
    @Test
    public void shouldReturnTrueForExistingFile() throws IOException {
      try (ZipFile zipFile = createTemporaryZipFile("TestExists", 2, 0)) {
        final MFileZip mFile = new MFileZip(zipFile.getName());
        assertThat(mFile.exists()).isEqualTo(true);
      }
    }

    @Test
    public void shouldGetLastModified() throws IOException {
      try (ZipFile zipFile = createTemporaryZipFile("TestLastModified", 20, 1)) {
        final MFileZip mFile = new MFileZip(zipFile.getName());
        assertThat(mFile.getLastModified()).isGreaterThan(0);
        assertThat(mFile.getLastModified()).isEqualTo(new File(zipFile.getName()).lastModified());
      }
    }

    @Test
    public void shouldGetLengthForZipFile() throws IOException {
      try (ZipFile zipFile = createTemporaryZipFile("TestLength", 30, 1)) {
        final MFileZip mFile = new MFileZip(zipFile.getName());
        assertThat(mFile.getLength()).isGreaterThan(30);
        assertThat(mFile.getLength()).isEqualTo(new File(zipFile.getName()).length());
      }
    }

    @Test
    public void shouldProvideForZipExtensions() {
      MFileZip.Provider provider = new MFileZip.Provider();
      assertThat(provider.canProvide("foo.zip")).isTrue();
      assertThat(provider.canProvide("foo.ZIP")).isTrue();
      assertThat(provider.canProvide("foo.zip2")).isTrue();
      assertThat(provider.canProvide("foo.txt")).isFalse();
    }
  }

  private static ZipFile createTemporaryZipFile(String name, int size, int numberOfFiles) throws IOException {
    final File zipFile = tempFolder.newFile(name + "-" + size + "-" + numberOfFiles + ".zip");

    try (FileOutputStream fos = new FileOutputStream(zipFile.getPath());
        ZipOutputStream zipOS = new ZipOutputStream(fos)) {
      for (int i = 0; i < numberOfFiles; i++) {
        final File file = createTemporaryFile(size);
        writeToZipFile(file, zipOS);
      }
    }

    return new ZipFile(zipFile.getPath());
  }

  private static void writeToZipFile(File file, ZipOutputStream zipStream) throws IOException {
    final ZipEntry zipEntry = new ZipEntry(file.getPath());
    zipStream.putNextEntry(zipEntry);
    zipStream.write(Files.readAllBytes(file.toPath()), 0, (int) file.length());
    zipStream.closeEntry();
  }

  private static File createTemporaryFile(int size) throws IOException {
    final File tempFile = tempFolder.newFile();

    byte[] bytes = new byte[size];
    new Random().nextBytes(bytes);
    Files.write(tempFile.toPath(), bytes);

    return tempFile;
  }
}
