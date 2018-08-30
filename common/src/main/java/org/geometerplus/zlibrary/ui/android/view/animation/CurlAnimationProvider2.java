/*
 * Copyright (C) 2007-2017 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.ui.android.view.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil;
import org.geometerplus.zlibrary.ui.android.view.ViewUtil;

public final class CurlAnimationProvider2 extends AnimationProvider {
	private final Paint myPaint = new Paint();
	private final Paint myBackPaint = new Paint();
	private final Paint myEdgePaint = new Paint();

	final Path myFgPath = new Path();
	final Path myEdgePath = new Path();
	final Path myQuadPath = new Path();

	private final Paint myPointPaint = new Paint();
	private final Paint myLinePaint = new Paint();
	private final Paint myTextPaint = new Paint();

	private float mySpeedFactor = 1;

	public CurlAnimationProvider2(BitmapManager bitmapManager) {
		super(bitmapManager);
		myPointPaint.setColor(Color.RED);

		myTextPaint.setTextSize(48);
		myTextPaint.setColor(Color.RED);

		myLinePaint.setStyle(Paint.Style.STROKE);
		myLinePaint.setColor(Color.BLUE);
		myLinePaint.setStrokeWidth(2);
		myLinePaint.setAntiAlias(true);

		myBackPaint.setAntiAlias(true);
		myBackPaint.setAlpha(0x40);

		myEdgePaint.setAntiAlias(true);
		myEdgePaint.setStyle(Paint.Style.FILL);
		myEdgePaint.setShadowLayer(15, 0, 0, 0xC0000000);
	}

	private Bitmap myBuffer;
	private volatile boolean myUseCanvasHack = true;

	@Override
	protected void drawInternal(Canvas canvas) {
		if (myUseCanvasHack) {
			// This is a hack that disables hardware acceleration
			//   1) for GLES20Canvas we got an UnsupportedOperationException in clipPath
			//   2) View.setLayerType(LAYER_TYPE_SOFTWARE) does not work properly in some cases
			if (myBuffer == null ||
					myBuffer.getWidth() != myWidth ||
					myBuffer.getHeight() != myHeight) {
				myBuffer = BitmapUtil.createBitmap(myWidth, myHeight, getBitmapTo().getConfig());
			}
			final Canvas softCanvas = new Canvas(myBuffer);
			//drawInternalNoHack(softCanvas);
			drawDebugInternalNoHack(softCanvas);
			canvas.drawBitmap(myBuffer, 0, 0, myPaint);
		} else {
			try {
				//drawInternalNoHack(canvas);
				drawDebugInternalNoHack(canvas);
			} catch (UnsupportedOperationException e) {
				myUseCanvasHack = true;
				drawInternal(canvas);
			}
		}
	}

	private void check(Canvas canvas){
		final int cornerX = myStartX > myWidth / 2 ? myWidth : 0;
		final int cornerY = myStartY > myHeight / 2 ? myHeight : 0;
		final int oppositeX = Math.abs(myWidth - cornerX);
		final int oppositeY = Math.abs(myHeight - cornerY);
		final int x, y;
		if (myDirection.IsHorizontal) {
			x = myEndX;
			if (getMode().Auto) {
				y = myEndY;
			} else {
				if (cornerY == 0) {
					y = Math.max(1, Math.min(myHeight / 2, myEndY));
				} else {
					y = Math.max(myHeight / 2, Math.min(myHeight - 1, myEndY));
				}
			}
		} else {
			y = myEndY;
			if (getMode().Auto) {
				x = myEndX;
			} else {
				if (cornerX == 0) {
					x = Math.max(1, Math.min(myWidth / 2, myEndX));
				} else {
					x = Math.max(myWidth / 2, Math.min(myWidth - 1, myEndX));
				}
			}
		}
		final int dX = Math.max(1, Math.abs(x - cornerX));
		final int dY = Math.max(1, Math.abs(y - cornerY));

		final int x1 = cornerX == 0
				? (dY * dY / dX + dX) / 2
				: cornerX - (dY * dY / dX + dX) / 2;
		final int y1 = cornerY == 0
				? (dX * dX / dY + dY) / 2
				: cornerY - (dX * dX / dY + dY) / 2;

		float sX, sY;
		{
			float d1 = x - x1;
			float d2 = y - cornerY;
			sX = (float) Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerX == 0) {
				sX = -sX;
			}
		}
		{
			float d1 = x - cornerX;
			float d2 = y - y1;
			sY = (float)Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerY == 0) {
				sY = -sY;
			}
		}
	}

	private void drawDebugInternalNoHack(Canvas canvas){
		drawBitmapTo(canvas, 0, 0, myPaint);

		//final int cornerX = myStartX > myWidth / 2 ? myWidth : 0;
		final int cornerX = myWidth;
		final int cornerY = myStartY > myHeight / 2 ? myHeight : 0;
		final int oppositeX = Math.abs(myWidth - cornerX);
		final int oppositeY = Math.abs(myHeight - cornerY);

		int x, y;
		if (myDirection.IsHorizontal) {
			x = myEndX;
			if (getMode().Auto) {
				y = myEndY;
			} else {
				if (cornerY == 0) {
					y = Math.max(1, Math.min(myHeight / 2, myEndY));
				} else {
					y = Math.max(myHeight / 2, Math.min(myHeight - 1, myEndY));
				}
			}

//			x = myEndX;
//			if (getMode().Auto) {
//				y = myEndY;
//			} else {
//				if (cornerY == 0) {
//					y = Math.max(1, Math.min(myHeight / 2, myEndY));
//				} else {
//					y = Math.max(myHeight / 2, Math.min(myHeight - 1, myEndY));
//				}
//			}
		} else {
			y = myEndY;
			if (getMode().Auto) {
				x = myEndX;
			} else {
				if (cornerX == 0) {
					x = Math.max(1, Math.min(myWidth / 2, myEndX));
				} else {
					x = Math.max(myWidth / 2, Math.min(myWidth - 1, myEndX));
				}
			}
		}

		final int dX = Math.max(1, Math.abs(x - cornerX));
		final int dY = Math.max(1, Math.abs(y - cornerY));

		final int x1 = cornerX == 0
				? (dY * dY / dX + dX) / 2
				: cornerX - (dY * dY / dX + dX) / 2;
		final int y1 = cornerY == 0
				? (dX * dX / dY + dY) / 2
				: cornerY - (dX * dX / dY + dY) / 2;

		float sX, sY;
		{
			float d1 = x - x1;
			float d2 = y - cornerY;
			sX = (float) Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerX == 0) {
				sX = -sX;
			}
		}
		{
			float d1 = x - cornerX;
			float d2 = y - y1;
			sY = (float)Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerY == 0) {
				sY = -sY;
			}
		}

		//
		float ax = x;
		float ay = y;

		float kx = (x + cornerX) / 2;
		float ky = (y + y1) / 2;

		float hx = cornerX;
		float hy = y1;

		float jx = cornerX;
		float jy = y1 - sY;

		float cx = x1 - sX;
		float cy = cornerY;

		float ex = x1;
		float ey = cornerY;

		float bx = (x + x1) / 2;
		float by = (y + cornerY) / 2;

		float ix = (x + 7 * cornerX) / 8;
		float iy = (y + 7 * y1 - 2 * sY) / 8;

		float dx = (x + 7 * x1 - 2 * sX) / 8;
		float dy = (y + 7 * cornerY) / 8;

		float kix = (x + 3 * cornerX) / 4;
		float kiy = (y + 3 * y1) / 4;

		float dbx = (x + 3 * x1) / 4;
		float dby = (y + 3 * cornerY) / 4;

		{
			myQuadPath.moveTo(cx, cy);
			myQuadPath.quadTo(ex, ey, bx, by);
			canvas.drawPath(myQuadPath, myEdgePaint);
			myQuadPath.rewind();
			myQuadPath.moveTo(kx, ky);
			myQuadPath.quadTo(hx, hy, jx, jy);
			canvas.drawPath(myQuadPath, myEdgePaint);
			myQuadPath.rewind();
		}

		{
			myFgPath.rewind();
			myFgPath.moveTo(ax, ay);
			myFgPath.lineTo(kx, ky);
			myFgPath.quadTo(hx, hy, jx, jy);
			if (Math.abs(y1 - sY - cornerY) < myHeight) {
				myFgPath.lineTo(cornerX, oppositeY);
			}

			myFgPath.lineTo(oppositeX, oppositeY);
			if (Math.abs(x1 - sX - cornerX) < myWidth) {
				myFgPath.lineTo(oppositeX, cornerY);
			}
			myFgPath.lineTo(cx, cy);
			myFgPath.quadTo(ex, ey, bx, by);
			myFgPath.lineTo(x, y);
			canvas.drawPath(myFgPath, myLinePaint);

			canvas.save();
			canvas.clipPath(myFgPath);
			drawBitmapFrom(canvas, 0, 0, myPaint);
			canvas.restore();
		}

		{
			myEdgePaint.setColor(ZLAndroidColorUtil.rgb(ZLAndroidColorUtil.getAverageColor(getBitmapFrom())));
			myEdgePath.rewind();
			myEdgePath.moveTo(ax, ay);
			myEdgePath.lineTo(kx,ky);
			myEdgePath.quadTo(kix, kiy, ix, iy);

			myEdgePath.lineTo(dx, dy);
			myEdgePath.quadTo(dbx, dby, bx, by);

			canvas.drawPath(myEdgePath, myEdgePaint);

			canvas.save();
			canvas.clipPath(myEdgePath);
			final Matrix m = new Matrix();
			m.postScale(1, -1);
			m.postTranslate(x - cornerX, y + cornerY);
			final float angle;
			if (cornerY == 0) {
				angle = -180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
			} else {
				angle = 180 - 180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
			}
			m.postRotate(angle, x, y);
			canvas.drawBitmap(getBitmapFrom(), m, myBackPaint);
			canvas.restore();
		}

		{
			//
			int radius = 4;
			canvas.drawCircle(ax, ay, radius, myPointPaint);
			canvas.drawCircle(kx, ky, radius, myPointPaint);
			canvas.drawCircle(hx, hy, radius, myPointPaint);
			canvas.drawCircle(jx, jy, radius, myPointPaint);
			canvas.drawCircle(cx, cy, radius, myPointPaint);
			canvas.drawCircle(ex, ey, radius, myPointPaint);
			canvas.drawCircle(bx, by, radius, myPointPaint);

			canvas.drawCircle(kix, kiy, radius, myPointPaint);
			canvas.drawCircle(ix, iy, radius, myPointPaint);

			canvas.drawCircle(dbx, dby, radius, myPointPaint);
			canvas.drawCircle(dx, dy, radius, myPointPaint);

			canvas.drawCircle(sX, sY, radius, myPointPaint);
			canvas.drawCircle(x1, y1, radius, myPointPaint);

			canvas.drawCircle(dX, dY, radius, myPointPaint);

			float textSize = myTextPaint.getTextSize();

			canvas.drawText("a", ax - textSize, ay, myTextPaint);
			canvas.drawText("k", kx - textSize, ky, myTextPaint);
			canvas.drawText("h", hx - textSize, hy, myTextPaint);
			canvas.drawText("j", jx - textSize, jy, myTextPaint);
			canvas.drawText("c", cx - textSize, cy, myTextPaint);
			canvas.drawText("e", ex - textSize, ey, myTextPaint);
			canvas.drawText("b", bx - textSize, by, myTextPaint);

			canvas.drawText("d", dx - textSize, dy, myTextPaint);
			canvas.drawText("i", ix - textSize, iy, myTextPaint);

			canvas.drawText("s", sX - textSize, sY, myTextPaint);
			canvas.drawText("x", x1 - textSize, y1, myTextPaint);
		}

		Point dp = evaluate(new Point((int)cx, (int)cy), new Point((int)ex, (int)ey), new Point((int)bx, (int)by), 0.5f);
		canvas.drawCircle(dp.x, dp.y, 4, myPointPaint);
	}

	private void drawInternalNoHack(Canvas canvas) {
		drawBitmapTo(canvas, 0, 0, myPaint);
		check(canvas);

		final int cornerX = myStartX > myWidth / 2 ? myWidth : 0;
		final int cornerY = myStartY > myHeight / 2 ? myHeight : 0;
		final int oppositeX = Math.abs(myWidth - cornerX);
		final int oppositeY = Math.abs(myHeight - cornerY);
		final int x, y;
		if (myDirection.IsHorizontal) {
			x = myEndX;
			if (getMode().Auto) {
				y = myEndY;
			} else {
				if (cornerY == 0) {
					y = Math.max(1, Math.min(myHeight / 2, myEndY));
				} else {
					y = Math.max(myHeight / 2, Math.min(myHeight - 1, myEndY));
				}
			}
		} else {
			y = myEndY;
			if (getMode().Auto) {
				x = myEndX;
			} else {
				if (cornerX == 0) {
					x = Math.max(1, Math.min(myWidth / 2, myEndX));
				} else {
					x = Math.max(myWidth / 2, Math.min(myWidth - 1, myEndX));
				}
			}
		}
		final int dX = Math.max(1, Math.abs(x - cornerX));
		final int dY = Math.max(1, Math.abs(y - cornerY));

		final int x1 = cornerX == 0
				? (dY * dY / dX + dX) / 2
				: cornerX - (dY * dY / dX + dX) / 2;
		final int y1 = cornerY == 0
				? (dX * dX / dY + dY) / 2
				: cornerY - (dX * dX / dY + dY) / 2;

		float sX, sY;
		{
			float d1 = x - x1;
			float d2 = y - cornerY;
			sX = (float) Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerX == 0) {
				sX = -sX;
			}
		}
		{
			float d1 = x - cornerX;
			float d2 = y - y1;
			sY = (float)Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerY == 0) {
				sY = -sY;
			}
		}

		myFgPath.rewind();
		myFgPath.moveTo(x, y);
		myFgPath.lineTo((x + cornerX) / 2, (y + y1) / 2);
		myFgPath.quadTo(cornerX, y1, cornerX, y1 - sY);
		if (Math.abs(y1 - sY - cornerY) < myHeight) {
			myFgPath.lineTo(cornerX, oppositeY);
		}
		myFgPath.lineTo(oppositeX, oppositeY);
		if (Math.abs(x1 - sX - cornerX) < myWidth) {
			myFgPath.lineTo(oppositeX, cornerY);
		}
		myFgPath.lineTo(x1 - sX, cornerY);
		myFgPath.quadTo(x1, cornerY, (x + x1) / 2, (y + cornerY) / 2);

		myQuadPath.moveTo(x1 - sX, cornerY);
		myQuadPath.quadTo(x1, cornerY, (x + x1) / 2, (y + cornerY) / 2);
		canvas.drawPath(myQuadPath, myEdgePaint);
		myQuadPath.rewind();
		myQuadPath.moveTo((x + cornerX) / 2, (y + y1) / 2);
		myQuadPath.quadTo(cornerX, y1, cornerX, y1 - sY);
		canvas.drawPath(myQuadPath, myEdgePaint);
		myQuadPath.rewind();

		canvas.save();
		canvas.clipPath(myFgPath);
		drawBitmapFrom(canvas, 0, 0, myPaint);
		canvas.restore();

		myEdgePaint.setColor(ZLAndroidColorUtil.rgb(ZLAndroidColorUtil.getAverageColor(getBitmapFrom())));

		myEdgePath.rewind();
		myEdgePath.moveTo(x, y);
		myEdgePath.lineTo(
				(x + cornerX) / 2,
				(y + y1) / 2
		);
		myEdgePath.quadTo(
				(x + 3 * cornerX) / 4,
				(y + 3 * y1) / 4,
				(x + 7 * cornerX) / 8,
				(y + 7 * y1 - 2 * sY) / 8
		);
		myEdgePath.lineTo(
				(x + 7 * x1 - 2 * sX) / 8,
				(y + 7 * cornerY) / 8
		);
		myEdgePath.quadTo(
				(x + 3 * x1) / 4,
				(y + 3 * cornerY) / 4,
				(x + x1) / 2,
				(y + cornerY) / 2
		);

		canvas.drawPath(myEdgePath, myEdgePaint);

		canvas.save();
		canvas.clipPath(myEdgePath);
		final Matrix m = new Matrix();
		m.postScale(1, -1);
		m.postTranslate(x - cornerX, y + cornerY);
		final float angle;
		if (cornerY == 0) {
			angle = -180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
		} else {
			angle = 180 - 180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
		}
		m.postRotate(angle, x, y);
		canvas.drawBitmap(getBitmapFrom(), m, myBackPaint);
		canvas.restore();
		/*
		canvas.save();
		canvas.clipPath(myEdgePath);
		final Matrix m = new Matrix();
		m.postScale(1, -1);
		m.postTranslate(x - cornerX, y + cornerY);
		final float angle;
		if (cornerY == 0) {
			angle = -180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
		} else {
			angle = 180 - 180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
		}
		m.postRotate(angle, x, y);
		canvas.drawBitmap(getBitmapFrom(), m, myBackPaint);
		canvas.restore();
		*/

		drawDebugInternalNoHack(canvas);
	}

	@Override
	public ZLViewEnums.PageIndex getPageToScrollTo(int x, int y) {
		if (myDirection == null) {
			return ZLViewEnums.PageIndex.current;
		}
		switch (myDirection) {
			case leftToRight:
				return myStartX < myWidth / 2 ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
			case rightToLeft:
				return myStartX < myWidth / 2 ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
			case up:
				return myStartY < myHeight / 2 ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
			case down:
				return myStartY < myHeight / 2 ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
		}
		return ZLViewEnums.PageIndex.current;
	}

	@Override
	protected void startAnimatedScrollingInternal(int speed) {
		mySpeedFactor = (float)Math.pow(2.0, 0.25 * speed);
		mySpeed *= 1.5;
		doStep();
	}

	boolean mHScrolling = false;

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		mHScrolling = false;
		if(x > myWidth / 2){
			int cel = myHeight / 3;
			if(y > cel && y < 2 * cel){
				if (myDirection.IsHorizontal) {
					x = mySpeed < 0 ? myWidth - 1 : 1;
					y = 1;
				} else {
					x = 1;
					y = mySpeed < 0 ? myHeight - 1 : 1;
				}
				myEndX = myStartX = x;
				myEndY = myStartY = y;
				mHScrolling = true;
				return;
			}
		}

		if (x == null || y == null) {
			if (myDirection.IsHorizontal) {
				x = mySpeed < 0 ? myWidth - 3 : 3;
				y = 1;
			} else {
				x = 1;
				y = mySpeed < 0 ? myHeight - 3 : 3;
			}
		} else {
			final int cornerX = x > myWidth / 2 ? myWidth : 0;
			final int cornerY = y > myHeight / 2 ? myHeight : 0;
			int deltaX = Math.min(Math.abs(x - cornerX), myWidth / 5);
			int deltaY = Math.min(Math.abs(y - cornerY), myHeight / 5);
			if (myDirection.IsHorizontal) {
				deltaY = Math.min(deltaY, deltaX / 3);
			} else {
				deltaX = Math.min(deltaX, deltaY / 3);
			}
			x = Math.abs(cornerX - deltaX);
			y = Math.abs(cornerY - deltaY);
		}
		myEndX = myStartX = x;
		myEndY = myStartY = y;
	}

	@Override
	public void doStep() {
		if (!getMode().Auto) {
			return;
		}

		final int speed = (int)Math.abs(mySpeed);
		mySpeed *= mySpeedFactor;

		final int cornerX = myStartX > myWidth / 2 ? myWidth : 0;
		final int cornerY = myStartY > myHeight / 2 ? myHeight : 0;

		final int boundX, boundY;
		if (getMode() == Mode.AnimatedScrollingForward) {
			boundX = cornerX == 0 ? 2 * myWidth : -myWidth;
			boundY = cornerY == 0 ? 2 * myHeight : -myHeight;
		} else {
			boundX = cornerX;
			boundY = cornerY;
		}

		final int deltaX = Math.abs(myEndX - cornerX);
		final int deltaY = Math.abs(myEndY - cornerY);
		final int speedX, speedY;
		if (deltaX == 0 || deltaY == 0) {
			speedX = speed;
			speedY = speed;
		} else if (deltaX < deltaY) {
			speedX = speed;
			speedY = speed * deltaY / deltaX;
		} else {
			speedX = speed * deltaX / deltaY;
			speedY = speed;
		}

		final boolean xSpeedIsPositive, ySpeedIsPositive;
		if (getMode() == Mode.AnimatedScrollingForward) {
			xSpeedIsPositive = cornerX == 0;
			ySpeedIsPositive = cornerY == 0;
		} else {
			xSpeedIsPositive = cornerX != 0;
			ySpeedIsPositive = cornerY != 0;
		}

		if (xSpeedIsPositive) {
			myEndX += speedX;
			if (myEndX >= boundX) {
				terminate();
				mHScrolling = false;
			}
		} else {
			myEndX -= speedX;
			if (myEndX <= boundX) {
				terminate();
				mHScrolling = false;
			}
		}

		if (ySpeedIsPositive) {
			if (mHScrolling) {
				myEndY = cornerY;
			}else{
				myEndY += speedY;
				if (myEndY >= boundY) {
					terminate();
				}
			}
		} else {
			if (mHScrolling) {
				myEndY = cornerY;
			}else{
				myEndY -= speedY;
				if (myEndY <= boundY) {
					terminate();
				}
			}
		}
	}

	@Override
	public void drawFooterBitmapInternal(Canvas canvas, Bitmap footerBitmap, int voffset) {
		canvas.drawBitmap(footerBitmap, 0, voffset, myPaint);
	}

	@Override
	protected void setFilter() {
		ViewUtil.setColorLevel(myPaint, myColorLevel);
		ViewUtil.setColorLevel(myBackPaint, myColorLevel);
		ViewUtil.setColorLevel(myEdgePaint, myColorLevel);
	}

	/**
	 * 直线P1P2和直线P3P4的交点坐标
	 */
	public PointF getCross(PointF P1, PointF P2, PointF P3, PointF P4) {
		PointF crossp = new PointF();
		float a1 = (P2.y - P1.y) / (P2.x - P1.x);
		float b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x);
		float a2 = (P4.y - P3.y) / (P4.x - P3.x);
		float b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x);
		crossp.x = (b2 - b1) / (a1 - a2);
		crossp.y = a1 * crossp.x + b1;
		return crossp;
	}

	public Point evaluate(Point p0, Point p1, Point p2, float t){
		Point point = new Point();
		point.x = evaluate(p0.x, p1.x, p2.x, t);
		point.y = evaluate(p0.y, p1.y, p2.y, t);
		return point;
	}

	public int evaluate(int p0, int p1, int p2, float t){
		return (int)((1-t) * (1-t) * p0 + 2 * t * (1-t) * p1 + t * t * p2);
	}
}
