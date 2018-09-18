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
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil;
import org.geometerplus.zlibrary.ui.android.view.ViewUtil;

public final class CurlAnimationProvider extends AnimationProvider {
	private final Paint myPaint = new Paint();
	private final Paint myBackPaint = new Paint();
	private final Paint myEdgePaint = new Paint();
	private final Paint myQuadPaint = new Paint();

	final Path myFgPath = new Path();
	final Path myEdgePath = new Path();
	final Path myQuadPath = new Path();

	private final Paint myPointPaint = new Paint();
	private final Paint myLinePaint = new Paint();
	private final Paint myTextPaint = new Paint();

	final Path path = new Path();
	private final Paint paint = new Paint();

	private float mySpeedFactor = 1;

	public CurlAnimationProvider(BitmapManager bitmapManager) {
		super(bitmapManager);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLUE);
		paint.setTextSize(4);
		paint.setStrokeWidth(2);
		paint.setAntiAlias(true);

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

		myQuadPaint.setStyle(Paint.Style.STROKE);
		myQuadPaint.setAntiAlias(true);
		myQuadPaint.setStrokeWidth(2);
		myQuadPaint.setShadowLayer(15, 0, 0, 0xC0000000);
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
			drawInternalNoHack(softCanvas);
			canvas.drawBitmap(myBuffer, 0, 0, myPaint);
		} else {
			try {
				drawInternalNoHack(canvas);
			} catch (UnsupportedOperationException e) {
				myUseCanvasHack = true;
				drawInternal(canvas);
			}
		}
		//drawDebugClickArea(canvas);
	}

	private void drawDebugClickArea(Canvas canvas){
		myLinePaint.setColor(Color.BLUE);

		Rect left = new Rect();
		left.left = 0;
		left.top = 0;
		left.right = myWidth * 1 / 3;
		left.bottom = myHeight;

		Rect top = new Rect();
		top.left = left.right;
		top.top = 0;
		top.right = myWidth;
		top.bottom = myHeight * 1 / 3;

		Rect mid = new Rect();
		mid.left = left.right;
		mid.top = top.bottom;
		mid.right = myWidth * 2 / 3;
		mid.bottom = myHeight * 2 / 3;

		Rect right = new Rect();
		right.left = mid.right;
		right.top = top.bottom;
		right.right = myWidth;
		right.bottom = myHeight * 2 / 3;

		Rect bottom = new Rect();
		bottom.left = left.right;
		bottom.top = mid.bottom;
		bottom.right = myWidth;
		bottom.bottom = myHeight;

		canvas.drawRect(left, myLinePaint);
		canvas.drawRect(top, myLinePaint);
		canvas.drawRect(right, myLinePaint);
		canvas.drawRect(bottom, myLinePaint);
	}

	private void drawInternalNoHack(Canvas canvas){
		final ZLViewEnums.PageIndex pageIndex = getPageToScrollTo();

		if(pageIndex == ZLViewEnums.PageIndex.next){
			drawBitmapTo(canvas, 0, 0, myPaint);
		}else{
			drawBitmapFrom(canvas, 0, 0, myPaint);
		}

		final int cornerX = myWidth;
		final int cornerY = myStartY > myHeight / 2 ? myHeight : 0;

		int oppositeX = Math.abs(myWidth - cornerX);
		final int oppositeY = Math.abs(myHeight - cornerY);

		float offsetX = myEndX - myStartX;
		float offsetY = myEndY - myStartY;

		float ox = pageIndex == ZLViewEnums.PageIndex.next ? cornerX + offsetX : offsetX;
		float oy = cornerY + offsetY;

		float circleX = oppositeX == 0 ? myWidth * 1/5f : oppositeX-myWidth *1/5f;
		float r = myWidth * 4/5f;
		boolean contains = circleContains(circleX, cornerY, r, ox, oy);
		if(!contains){
			PointF point = circleCross(circleX, cornerY, r, ox, oy);
			ox = (int)point.x;
			oy = (int)point.y;
		}

		oy = Math.max(1, Math.min(myHeight - 1, oy));

		// 向量线段中点计算公式
		float opx = (cornerX + ox)/2;
		float opy = (cornerY + oy)/2;

		float ax = opx - (cornerY-opy) * (cornerY-opy) / (cornerX-opx);
		float ay = cornerY;

		float bx = cornerX;
		float by = opy - (cornerX-opx) * (cornerX-opx) / (cornerY-opy);

		// 向量线段定比分点计算公式 OF = 3/4FA
		float fx = (ox + 3 * ax) / 4;
		float fy = (oy + 3 * ay) / 4;

		// 向量线段定比分点计算公式 OD = 3/4DB
		float dx = (ox + 3 * bx) / 4;
		float dy = (oy + 3 * by) / 4;

		// 因为AF=AG, AF=1/4AO, AO=AP=(cornerX - ax),
		// 所以AG=1/4(cornerX - ax)
		float gx = ax - 1/4f * (cornerX - ax);
		float gy = cornerY;

		float cx = cornerX;
		float cy = by - 1/4f * (cornerY - by);

		float ix = bezier2(gx, ax, fx, 0.5f);
		float iy = bezier2(gy, ay, fy, 0.5f);

		float jx = bezier2(dx, bx, cx, 0.5f);
		float jy = bezier2(dy, by, cy, 0.5f);

		{
			myQuadPath.moveTo(gx, gy);
			myQuadPath.quadTo(ax, ay, fx, fy);
			canvas.drawPath(myQuadPath, myEdgePaint);
			myQuadPath.rewind();
			myQuadPath.moveTo(dx, dy);
			myQuadPath.quadTo(bx, by, cx, cy);
			canvas.drawPath(myQuadPath, myEdgePaint);
			myQuadPath.rewind();
		}

		{
			myFgPath.rewind();
			myFgPath.moveTo(ox, oy);
			myFgPath.lineTo(dx, dy);
			myFgPath.quadTo(bx, by, cx, cy);

			myFgPath.lineTo(cornerX, oppositeY);
			myFgPath.lineTo(oppositeX, oppositeY);
			myFgPath.lineTo(oppositeX, cornerY);

			myFgPath.lineTo(gx, gy);
			myFgPath.quadTo(ax, ay, fx, fy);
			myFgPath.lineTo(ax, ay);

			canvas.save();
			canvas.clipPath(myFgPath);

			if(pageIndex == ZLViewEnums.PageIndex.next){
				drawBitmapFrom(canvas, 0, 0, myPaint);
			}else{
				drawBitmapTo(canvas, 0, 0, myPaint);
			}
			canvas.restore();
		}

		{
			myEdgePaint.setColor(ZLAndroidColorUtil.rgb(ZLAndroidColorUtil.getAverageColor(getBitmapFrom())));
			myEdgePath.reset();
			myEdgePath.moveTo(ix, iy);
			myEdgePath.quadTo((fx+ax)/2, (fy+ay)/2, fx, fy);
			myEdgePath.lineTo(ox, oy);
			myEdgePath.lineTo(dx, dy);
			myEdgePath.quadTo((dx+bx)/2, (dy+by)/2, jx, jy);
			canvas.drawPath(myEdgePath, myEdgePaint);

			canvas.save();
			canvas.clipPath(myEdgePath);
			final Matrix m = new Matrix();
			m.postScale(1, -1);
			m.postTranslate(ox - cornerX, oy + cornerY);
			final float angle;
			if (cornerY == 0) {
				angle = -180 / 3.1416f * (float)Math.atan2(ox - cornerX, oy - (dy+by)/2);
			} else {
				angle = 180 - 180 / 3.1416f * (float)Math.atan2(ox - cornerX, oy - (dy+by)/2);
			}
			m.postRotate(angle, ox, oy);
			canvas.drawBitmap(pageIndex == ZLViewEnums.PageIndex.next ? getBitmapFrom() : getBitmapTo(), m, myBackPaint);
			canvas.restore();
		}
	}

	private Region computeRegion(Path path) {
		Region region = new Region();
		RectF f = new RectF();
		path.computeBounds(f, true);
		region.setPath(path, new Region((int) f.left, (int) f.top, (int) f.right, (int) f.bottom));
		return region;
	}

	@Override
	public void startManualScrolling(int x, int y) {
		super.startManualScrolling(x, y);
	}

	@Override
	protected void startAnimatedScrollingInternal(int speed) {
		mySpeedFactor = (float)Math.pow(2.0, 0.25 * speed);
		mySpeed *= 1.5;
		doStep();
	}

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		if (x == null || y == null) {
			if (myDirection.IsHorizontal) {
				x = mySpeed < 0 ? myWidth - 3 : 3;
				y = 1;
			} else {
				x = 1;
				y = mySpeed < 0 ? myHeight - 3 : 3;
			}
		} else {
//			final int cornerX = x > myWidth / 2 ? myWidth : 0;
//			final int cornerY = y > myHeight / 2 ? myHeight : 0;
//			int deltaX = Math.min(Math.abs(x - cornerX), myWidth / 5);
//			int deltaY = Math.min(Math.abs(y - cornerY), myHeight / 5);
//			if (myDirection.IsHorizontal) {
//				deltaY = Math.min(deltaY, deltaX / 3);
//			} else {
//				deltaX = Math.min(deltaX, deltaY / 3);
//			}
//			x = Math.abs(cornerX - deltaX);
//			y = Math.abs(cornerY - deltaY);
		}
		myEndX = myStartX = x;
		myEndY = myStartY = y;
	}

	@Override
	public void doStep() {
		if (!getMode().Auto) {
			return;
		}

		myEndX += (int)mySpeed;

		final int boundX = myWidth;

		if (mySpeed > 0) {
			if (myEndX >= boundX) {
				terminate();
				return;
			}
		} else {
			if (myEndX <= -boundX) {
				terminate();
				return;
			}
		}
		mySpeed *= mySpeedFactor;
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

	public Point bezier2(Point p0, Point p1, Point p2, float t){
		Point point = new Point();
		point.x = bezier2(p0.x, p1.x, p2.x, t);
		point.y = bezier2(p0.y, p1.y, p2.y, t);
		return point;
	}

	public int bezier2(int p0, int p1, int p2, float t){
		return (int)((1-t) * (1-t) * p0 + 2 * t * (1-t) * p1 + t * t * p2);
	}

	public float bezier2(float p0, float p1, float p2, float t){
		return (1-t) * (1-t) * p0 + 2 * t * (1-t) * p1 + t * t * p2;
	}

	/**
	 * 已知起始点，t时间上一点，终点，求控制点
	 * @param p0 起始点
	 * @param pt  t时间上一点
	 * @param p2 终点
	 * @param t
	 * @return
	 */
	public float bezier3(float p0, float pt, float p2, float t){
		// c = (1-t) * (1-t) * p0 + 2 * t * (1-t) * p1 + t * t * p2
		// c - (1-t) * (1-t) * p0 - t * t * p2 = 2 * t * (1-t) * p1
		// p1 = (c - (1-t) * (1-t) * p0 - t * t * p2) / 2 * t * (1-t)
		return (pt - (1-t) * (1-t) * p0 - t * t * p2) / (2 * t * (1-t));
	}

	public static boolean circleContains(float a, float b, float radius, float x, float y) {
		x = a - x;
		y = b - y;
		return x * x + y * y < radius * radius;
	}

	/**
	 * 求点(x,y)过圆心与圆相交的一点
	 * @param a 圆心x
	 * @param b 圆心y
	 * @param r 圆半径
	 * @param x 需要求的点x
	 * @param y 需要求的点y
	 * @return
	 */
	public static PointF circleCross(float a, float b, float r, float x, float y){

		float C = x - a;
		float D = y - b;

		float R = r;

		float cx1 = (float)Math.pow(C*C*R*R/(C*C+D*D), 0.5f);
		float cy1 = (float)Math.pow(D*D*R*R/(C*C+D*D), 0.5f);

		float p1x = cx1 + a;
		float p1y = cy1 + b;
		float p2x = -cx1 + a;
		float p2y = -cy1 + b;

		PointF point = new PointF();
		final int quadrant = getQuadrant(a, b, x, y);
		switch (quadrant){
			case 1:
				point.set(p1x, p1y);
				break;
			case 2:
				point.set(p2x, p1y);
				break;
			case 3:
				point.set(p2x, p2y);
				break;
			case 4:
				point.set(p1x, p2y);
				break;
			default:
				break;
		}
		return point;
	}

	private static int getQuadrant(float a, float b, float x, float y){
		if(x >= a && y >=b){
			return 1;
		}else if(x <= a && y >=b){
			return 2;
		}else if(x <= a && y <=b){
			return 3;
		}else if(x >= a && y <=b){
			return 4;
		}
		return 0;
	}

	/**
	 * 两点距离
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static float pdist(float x1, float y1, float x2, float y2){
		final float x = Math.abs(x1 - x2);
		final float y = Math.abs(y1 - y2);
		return (float)Math.sqrt(x * x + y * y);
	}
}
