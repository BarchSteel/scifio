/*
 * #%L
 * OME SCIFIO package for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2005 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package io.scif;

import io.scif.FormatException;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;
import io.scif.util.SCIFIOMetadataTools;

import java.io.File;
import java.io.IOException;

import net.imglib2.meta.Axes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract superclass of all SCIFIO {@link io.scif.Reader} implementations.
 * 
 * @see io.scif.Reader
 * @see io.scif.HasFormat
 * @see io.scif.Metadata
 * @see io.scif.Plane
 *
 * @author Mark Hiner
 */
public abstract class AbstractReader<M extends TypedMetadata, P extends DataPlane<?>>
  extends AbstractGroupable implements TypedReader<M, P> {

  // -- Constants --

  protected static final Logger LOGGER = LoggerFactory.getLogger(Reader.class);

  // -- Fields --

  /** Metadata for the current image source. */
  protected M metadata;

  /** Whether or not to normalize float data. */
  protected boolean normalizeData;

  /** List of domains in which this format is used. */
  protected String[] domains = new String[0];

  /** Name of current file. */
  protected String currentId;

  /** Whether this format supports multi-file datasets. */
  protected boolean hasCompanionFiles = false;
  
  private Class<P> planeClass;

  // -- Constructors --

  /** Constructs a reader and stores a reference to its plane type */
  public AbstractReader(Class<P> planeClass)
  {
    this.planeClass = planeClass;
  }

  // -- Reader API Methods --

  //TODO Merge common Reader and Writer API methods

  /*
   * @see io.scif.Reader#openPlane(int, int)
   */
  public P openPlane(final int imageIndex, final int planeNumber)
    throws FormatException, IOException
  {
    return openPlane(imageIndex, planeNumber, 0, 0,
      metadata.getAxisLength(imageIndex, Axes.X),
      metadata.getAxisLength(imageIndex, Axes.Y));
  }

  /*
   * @see io.scif.Reader#openPlane(int, int, int, int, int, int)
   */
  public P openPlane(final int imageIndex, final int planeIndex,
    final int x, final int y, final int w, final int h)
    throws FormatException, IOException
  {
    P plane = null;
    
    try {
      plane = createPlane(x, y, w, h);
    }
    catch (IllegalArgumentException e) {
      throw new FormatException("Image plane too large. Only 2GB of data can " +
          "be extracted at one time. You can workaround the problem by opening " +
          "the plane in tiles; for further details, see: " +
          "http://www.openmicroscopy.org/site/support/faq/bio-formats/" +
          "i-see-an-outofmemory-or-negativearraysize-error-message-when-" +
          "attempting-to-open-an-svs-or-jpeg-2000-file.-what-does-this-mean", e);
    }
 
    return openPlane(imageIndex, planeIndex, plane, x, y, w, h);
  }

  /*
   * @see io.scif.Reader#openPlane(int, int, io.scif.Plane)
   */
  public P openPlane(int imageIndex, int planeIndex, Plane plane)
      throws FormatException, IOException {
    return openPlane(imageIndex, planeIndex, this.<P>castToTypedPlane(plane));
  }

  /*
   * @see io.scif.Reader#openPlane(int, int, io.scif.Plane, int, int, int, int)
   */
  public P openPlane(int imageIndex, int planeIndex, Plane plane, int x,
      int y, int w, int h) throws FormatException, IOException {
    return openPlane(imageIndex, planeIndex, this.<P>castToTypedPlane(plane), x, y, w, h);
  }

  /*
   * @see io.scif.Reader#getCurrentFile()
   */
  public String getCurrentFile() {
    return getStream() == null ? null : getStream().getFileName();
  }

  /*
   * @see io.scif.Reader#getDomains()
   */
  public String[] getDomains() {
    return domains;
  }

  /*
   * @see io.scif.Reader#getStream()
   */
  public RandomAccessInputStream getStream() {
    return metadata == null ? null : metadata.getSource();
  }

  /*
   * @see io.scif.Reader#getUnderlyingReaders()
   */
  public Reader[] getUnderlyingReaders() {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * @see io.scif.Reader#getOptimalTileWidth(int)
   */
  public int getOptimalTileWidth(final int imageIndex) {
    return metadata.getAxisLength(imageIndex, Axes.X);
  }

  /*
   * @see io.scif.Reader#getOptimalTileHeight(int)
   */
  public int getOptimalTileHeight(final int imageIndex) {
    final int bpp =
      FormatTools.getBytesPerPixel(metadata.getPixelType(imageIndex));
    
    final int width = metadata.getAxisLength(imageIndex, Axes.X);
    final int rgbcCount = metadata.getRGBChannelCount(imageIndex);
    
    final int maxHeight = 
      (1024 * 1024) / (width * rgbcCount * bpp);
    return Math.min(maxHeight, metadata.getAxisLength(imageIndex, Axes.Y));
  }

  /*
   * @see io.scif.Reader#setMetadata(io.scif.Metadata)
   */
  public void setMetadata(io.scif.Metadata meta) throws IOException {
    setMetadata(SCIFIOMetadataTools.<M>castMeta(meta));
  }

  /*
   * @see io.scif.Reader#getMetadata()
   */
  public M getMetadata() {
    return metadata;
  }

  /*
   * @see io.scif.Reader#setNormalized(boolean)
   */
  public void setNormalized(final boolean normalize) {
    normalizeData = normalize;
  }

  /*
   * @see io.scif.Reader#isNormalized()
   */
  public boolean isNormalized() {
    return normalizeData;
  }

  /*
   * @see io.scif.Reader#hasCompanionFiles()
   */
  public boolean hasCompanionFiles() {
    return hasCompanionFiles;
  }

  /*
   * @see io.scif.Reader#setSource(java.lang.String)
   */
  public void setSource(final String fileName) throws IOException {

    if (getStream() != null && getStream().getFileName() != null &&
        getStream().getFileName().equals(fileName)) {
      getStream().seek(0);
      return;
    }
    else {
      close();
      RandomAccessInputStream stream = new RandomAccessInputStream(getContext(), fileName);
      try {
        setMetadata(getFormat().createParser().parse(stream));
      } catch (FormatException e) {
        throw new IOException(e);
      }
      setSource(stream);
    }
  }
  
  /*
   * @see io.scif.Reader#setSource(java.io.File)
   */
  public void setSource(final File file) throws IOException {
    setSource(file.getName());
  }

  /*
   * @see io.scif.Reader#setSource(io.scif.io.RandomAccessInputStream)
   */
  public void setSource(final RandomAccessInputStream stream)
    throws IOException
  {
    if (metadata != null && getStream() != stream)
      close();

    if (metadata == null) {
      currentId = stream.getFileName();
      
      try {
        @SuppressWarnings("unchecked")
        final M meta = (M) getFormat().createParser().parse(stream);
        setMetadata(meta);
      }
      catch (final FormatException e) {
        throw new IOException(e);
      }
    }
  }


  /*
   * @see io.scif.Reader#readPlane(io.scif.io.RandomAccessInputStream, int, int, int, int, int, io.scif.Plane)
   */
  public Plane readPlane(RandomAccessInputStream s, int imageIndex, int x,
      int y, int w, int h, Plane plane) throws IOException {
    return readPlane(s, imageIndex, x, y, w, h, this.<P>castToTypedPlane(plane));
  }

  /*
   * @see io.scif.Reader#readPlane(io.scif.io.RandomAccessInputStream, int, int, int, int, int, int, io.scif.Plane)
   */
  public Plane readPlane(RandomAccessInputStream s, int imageIndex, int x,
      int y, int w, int h, int scanlinePad, Plane plane) throws IOException {
    return readPlane(s, imageIndex, x, y, w, h, scanlinePad, this.<P>castToTypedPlane(plane));
  }

  /*
   * @see io.scif.Reader#getPlaneCount(int)
   */
  public int getPlaneCount(final int imageIndex) {
    return metadata.getPlaneCount(imageIndex);
  }

  /*
   * @see io.scif.Reader#getImageCount()
   */
  public int getImageCount() {
    return metadata.getImageCount();
  }
  
  /*
   * @see io.scif.Reader#castToTypedPlane(io.scif.Plane)
   */
  public <T extends Plane> T castToTypedPlane(Plane plane) {
    if(!planeClass.isAssignableFrom(plane.getClass())) {
      throw new IllegalArgumentException("Incompatible plane types. " +
          "Attempted to cast: " + plane.getClass() + " to: " + planeClass);
    }
      
    @SuppressWarnings("unchecked")
    T p = (T)plane;
    return p;
  }
  
  // -- TypedReader API --

  /*
   * @see io.scif.TypedReader#openPlane(int, int, io.scif.DataPlane)
   */
  public P openPlane(final int imageIndex, final int planeIndex,
    final P plane) throws FormatException, IOException
  {
    return openPlane(
      imageIndex, planeIndex, plane, 0, 0,
      metadata.getAxisLength(imageIndex, Axes.X),
      metadata.getAxisLength(imageIndex, Axes.Y));
  }
  
  /*
   * @see io.scif.TypedReader#setMetadata(io.scif.TypedMetadata)
   */
  public void setMetadata(final M meta) throws IOException {
    if (metadata != null && metadata != meta) { 
      close();
    }
    
    if (metadata == null) metadata = meta;
  }
  
  /*
   * @see io.scif.TypedReader#readPlane(io.scif.io.RandomAccessInputStream, int, int, int, int, int, io.scif.DataPlane)
   */
  public P readPlane(final RandomAccessInputStream s,
    final int imageIndex, final int x, final int y, final int w, final int h,
    final P plane) throws IOException
  {
    return readPlane(s, imageIndex, x, y, w, h, 0, plane);
  }

  /*
   * @see io.scif.TypedReader#readPlane(io.scif.io.RandomAccessInputStream, int, int, int, int, int, int, io.scif.DataPlane)
   */
  public P readPlane(final RandomAccessInputStream s,
    final int imageIndex, final int x, final int y, final int w, final int h,
    final int scanlinePad, final P plane) throws IOException
  {
    final int c = metadata.getRGBChannelCount(imageIndex);
    final int bpp =
      FormatTools.getBytesPerPixel(metadata.getPixelType(imageIndex));
    
    byte[] bytes = plane.getBytes();
    
    if (x == 0 && y == 0 && w == metadata.getAxisLength(imageIndex, Axes.X) &&
      h == metadata.getAxisLength(imageIndex, Axes.Y) && scanlinePad == 0)
    {
      s.read(bytes);
    }
    else if (x == 0 && w == metadata.getAxisLength(imageIndex, Axes.X) &&
      scanlinePad == 0)
    {
      if (metadata.isInterleaved(imageIndex)) {
        s.skipBytes(y * w * bpp * c);
        s.read(bytes, 0, h * w * bpp * c);
      }
      else {
        final int rowLen = w * bpp;
        for (int channel = 0; channel < c; channel++) {
          s.skipBytes(y * rowLen);
          s.read(bytes, channel * h * rowLen, h * rowLen);
          if (channel < c - 1) {
            // no need to skip bytes after reading final channel
            s.skipBytes((metadata.getAxisLength(imageIndex, Axes.Y) - y - h) *
              rowLen);
          }
        }
      }
    }
    else {
      final int scanlineWidth =
          metadata.getAxisLength(imageIndex, Axes.X) + scanlinePad;
      if (metadata.isInterleaved(imageIndex)) {
        s.skipBytes(y * scanlineWidth * bpp * c);
        for (int row = 0; row < h; row++) {
          s.skipBytes(x * bpp * c);
          s.read(bytes, row * w * bpp * c, w * bpp * c);
          if (row < h - 1) {
            // no need to skip bytes after reading final row
            s.skipBytes(bpp * c * (scanlineWidth - w - x));
          }
        }
      }
      else {
        for (int channel = 0; channel < c; channel++) {
          s.skipBytes(y * scanlineWidth * bpp);
          for (int row = 0; row < h; row++) {
            s.skipBytes(x * bpp);
            s.read(bytes, channel * w * h * bpp + row * w * bpp, w * bpp);
            if (row < h - 1 || channel < c - 1) {
              // no need to skip bytes after reading final row of final channel
              s.skipBytes(bpp * (scanlineWidth - w - x));
            }
          }
          if (channel < c - 1) {
            // no need to skip bytes after reading final channel
            s.skipBytes(scanlineWidth * bpp *
              (metadata.getAxisLength(imageIndex, Axes.Y) - y - h));
          }
        }
      }
    }
    return plane;
  }
    
  /*
   * @see io.scif.TypedReader#getPlaneClass()
   */
  public Class<P> getPlaneClass() {
    return planeClass;
  }

  // -- HasSource Format API --
  
  /*
   * @see io.scif.Reader#close(boolean)
   */
  public void close(final boolean fileOnly) throws IOException {
    if (metadata != null) metadata.close(fileOnly);
    
    if (!fileOnly) {
      metadata = null;
      currentId = null;
    }
  } 
}