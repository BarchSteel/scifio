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

package io.scif.io.img.cell;

import io.scif.FormatException;
import io.scif.Reader;

import java.io.IOException;

import net.imglib2.display.ColorTable;
import net.imglib2.img.cell.AbstractCell;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cells;
import net.imglib2.type.NativeType;

/**
 * {@link AbstractCellImg} implementation for working with
 * {@link SCIFIOCells}.
 * 
 * @author Mark Hiner hinerm at gmail.com
 *
 */
public class SCIFIOCellImg< T extends NativeType< T >, A, C extends AbstractCell<A> > extends AbstractCellImg< T, A, C, SCIFIOCellImgFactory< T > >
{

  private Reader reader;

  public SCIFIOCellImg(final SCIFIOCellImgFactory<T> factory,
      final Cells<A, C> cells) {
    super(factory, cells);
    reader = factory.reader();
  }

  // -- SCIFIOCellImg methods --

  public ColorTable getColorTable(int imageIndex, int planeIndex)
    throws FormatException, IOException {
    return reader.openPlane(imageIndex, planeIndex, 0, 0, 1, 1).getColorTable();
  }

  @SuppressWarnings("unchecked")
  public A update(final Object cursor) {
    return ((CellContainerSampler<T, A, C>) cursor).getCell().getData();
  }

  @Override
  public SCIFIOCellImgFactory<T> factory() {
    return (SCIFIOCellImgFactory<T>) factory;
  }

  /*
   * @see net.imglib2.img.Img#copy()
   */
  public SCIFIOCellImg< T, A, C > copy() {
    final SCIFIOCellImg<T, A, C> copy = (SCIFIOCellImg<T, A, C>) factory()
        .create(dimension, firstElement().createVariable());
    super.copyDataTo(copy);
    return copy;
  }
}