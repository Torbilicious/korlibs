/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dragonbones.model

/**
 * - The base class of bounding box data.
 * @see dragonBones.RectangleData
 * @see dragonBones.EllipseData
 * @see dragonBones.PolygonData
 * @version DragonBones 5.0
 * @language en_US
 */
/**
 * - 边界框数据基类。
 * @see dragonBones.RectangleData
 * @see dragonBones.EllipseData
 * @see dragonBones.PolygonData
 * @version DragonBones 5.0
 * @language zh_CN
 */
abstract class BoundingBoxData  :  BaseObject {
	/**
	 * - The bounding box type.
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 边界框类型。
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public type: BoundingBoxType;
	/**
	 * @private
	 */
	public color: Double;
	/**
	 * @private
	 */
	public width: Double;
	/**
	 * @private
	 */
	public height: Double;

	protected _onClear(): Unit {
		this.color = 0x000000;
		this.width = 0.0;
		this.height = 0.0;
	}
	/**
	 * - Check whether the bounding box contains a specific point. (Local coordinate system)
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查边界框是否包含特定点。（本地坐标系）
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public abstract containsPoint(pX: Double, pY: Double): Boolean;
	/**
	 * - Check whether the bounding box intersects a specific segment. (Local coordinate system)
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 检查边界框是否与特定线段相交。（本地坐标系）
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public abstract intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: { x: Double, y: Double }?,
		intersectionPointB: { x: Double, y: Double }?,
		normalRadians: { x: Double, y: Double }?
	): Double;
}
/**
 * - Cohen–Sutherland algorithm https://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm
 * ----------------------
 * | 0101 | 0100 | 0110 |
 * ----------------------
 * | 0001 | 0000 | 0010 |
 * ----------------------
 * | 1001 | 1000 | 1010 |
 * ----------------------
 */
val enum OutCode {
	InSide = 0, // 0000
	Left = 1,   // 0001
	Right = 2,  // 0010
	Top = 4,    // 0100
	Bottom = 8  // 1000
}
/**
 * - The rectangle bounding box data.
 * @version DragonBones 5.1
 * @language en_US
 */
/**
 * - 矩形边界框数据。
 * @version DragonBones 5.1
 * @language zh_CN
 */
class RectangleBoundingBoxData  :  BoundingBoxData {
	public static toString(): String {
		return "[class dragonBones.RectangleBoundingBoxData]";
	}
	/**
	 * - Compute the bit code for a point (x, y) using the clip rectangle
	 */
	private static _computeOutCode(x: Double, y: Double, xMin: Double, yMin: Double, xMax: Double, yMax: Double): Double {
		var code = OutCode.InSide;  // initialised as being inside of [[clip window]]

		if (x < xMin) {             // to the left of clip window
			code |= OutCode.Left;
		}
		else if (x > xMax) {        // to the right of clip window
			code |= OutCode.Right;
		}

		if (y < yMin) {             // below the clip window
			code |= OutCode.Top;
		}
		else if (y > yMax) {        // above the clip window
			code |= OutCode.Bottom;
		}

		return code;
	}
	/**
	 * @private
	 */
	public static rectangleIntersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		xMin: Double, yMin: Double, xMax: Double, yMax: Double,
		intersectionPointA: { x: Double, y: Double }? = null,
		intersectionPointB: { x: Double, y: Double }? = null,
		normalRadians: { x: Double, y: Double }? = null
	): Double {
		val inSideA = xA > xMin && xA < xMax && yA > yMin && yA < yMax;
		val inSideB = xB > xMin && xB < xMax && yB > yMin && yB < yMax;

		if (inSideA && inSideB) {
			return -1;
		}

		var intersectionCount = 0;
		var outcode0 = RectangleBoundingBoxData._computeOutCode(xA, yA, xMin, yMin, xMax, yMax);
		var outcode1 = RectangleBoundingBoxData._computeOutCode(xB, yB, xMin, yMin, xMax, yMax);

		while (true) {
			if ((outcode0 | outcode1) === 0) { // Bitwise OR is 0. Trivially accept and get out of loop
				intersectionCount = 2;
				break;
			}
			else if ((outcode0 & outcode1) !== 0) { // Bitwise AND is not 0. Trivially reject and get out of loop
				break;
			}

			// failed both tests, so calculate the line segment to clip
			// from an outside point to an intersection with clip edge
			var x = 0.0;
			var y = 0.0;
			var normalRadian = 0.0;

			// At least one endpoint is outside the clip rectangle; pick it.
			val outcodeOut = outcode0 !== 0 ? outcode0 : outcode1;

			// Now find the intersection point;
			if ((outcodeOut & OutCode.Top) !== 0) {             // point is above the clip rectangle
				x = xA + (xB - xA) * (yMin - yA) / (yB - yA);
				y = yMin;

				if (normalRadians !== null) {
					normalRadian = -Math.PI * 0.5;
				}
			}
			else if ((outcodeOut & OutCode.Bottom) !== 0) {     // point is below the clip rectangle
				x = xA + (xB - xA) * (yMax - yA) / (yB - yA);
				y = yMax;

				if (normalRadians !== null) {
					normalRadian = Math.PI * 0.5;
				}
			}
			else if ((outcodeOut & OutCode.Right) !== 0) {      // point is to the right of clip rectangle
				y = yA + (yB - yA) * (xMax - xA) / (xB - xA);
				x = xMax;

				if (normalRadians !== null) {
					normalRadian = 0;
				}
			}
			else if ((outcodeOut & OutCode.Left) !== 0) {       // point is to the left of clip rectangle
				y = yA + (yB - yA) * (xMin - xA) / (xB - xA);
				x = xMin;

				if (normalRadians !== null) {
					normalRadian = Math.PI;
				}
			}

			// Now we move outside point to intersection point to clip
			// and get ready for next pass.
			if (outcodeOut === outcode0) {
				xA = x;
				yA = y;
				outcode0 = RectangleBoundingBoxData._computeOutCode(xA, yA, xMin, yMin, xMax, yMax);

				if (normalRadians !== null) {
					normalRadians.x = normalRadian;
				}
			}
			else {
				xB = x;
				yB = y;
				outcode1 = RectangleBoundingBoxData._computeOutCode(xB, yB, xMin, yMin, xMax, yMax);

				if (normalRadians !== null) {
					normalRadians.y = normalRadian;
				}
			}
		}

		if (intersectionCount) {
			if (inSideA) {
				intersectionCount = 2; // 10

				if (intersectionPointA !== null) {
					intersectionPointA.x = xB;
					intersectionPointA.y = yB;
				}

				if (intersectionPointB !== null) {
					intersectionPointB.x = xB;
					intersectionPointB.y = xB;
				}

				if (normalRadians !== null) {
					normalRadians.x = normalRadians.y + Math.PI;
				}
			}
			else if (inSideB) {
				intersectionCount = 1; // 01

				if (intersectionPointA !== null) {
					intersectionPointA.x = xA;
					intersectionPointA.y = yA;
				}

				if (intersectionPointB !== null) {
					intersectionPointB.x = xA;
					intersectionPointB.y = yA;
				}

				if (normalRadians !== null) {
					normalRadians.y = normalRadians.x + Math.PI;
				}
			}
			else {
				intersectionCount = 3; // 11
				if (intersectionPointA !== null) {
					intersectionPointA.x = xA;
					intersectionPointA.y = yA;
				}

				if (intersectionPointB !== null) {
					intersectionPointB.x = xB;
					intersectionPointB.y = yB;
				}
			}
		}

		return intersectionCount;
	}

	protected _onClear(): Unit {
		super._onClear();

		this.type = BoundingBoxType.Rectangle;
	}
	/**
	 * @inheritDoc
	 */
	public containsPoint(pX: Double, pY: Double): Boolean {
		val widthH = this.width * 0.5;
		if (pX >= -widthH && pX <= widthH) {
			val heightH = this.height * 0.5;
			if (pY >= -heightH && pY <= heightH) {
				return true;
			}
		}

		return false;
	}
	/**
	 * @inheritDoc
	 */
	public intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: { x: Double, y: Double }? = null,
		intersectionPointB: { x: Double, y: Double }? = null,
		normalRadians: { x: Double, y: Double }? = null
	): Double {
		val widthH = this.width * 0.5;
		val heightH = this.height * 0.5;
		val intersectionCount = RectangleBoundingBoxData.rectangleIntersectsSegment(
			xA, yA, xB, yB,
			-widthH, -heightH, widthH, heightH,
			intersectionPointA, intersectionPointB, normalRadians
		);

		return intersectionCount;
	}
}
/**
 * - The ellipse bounding box data.
 * @version DragonBones 5.1
 * @language en_US
 */
/**
 * - 椭圆边界框数据。
 * @version DragonBones 5.1
 * @language zh_CN
 */
class EllipseBoundingBoxData  :  BoundingBoxData {
	public static toString(): String {
		return "[class dragonBones.EllipseData]";
	}
	/**
	 * @private
	 */
	public static ellipseIntersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		xC: Double, yC: Double, widthH: Double, heightH: Double,
		intersectionPointA: { x: Double, y: Double }? = null,
		intersectionPointB: { x: Double, y: Double }? = null,
		normalRadians: { x: Double, y: Double }? = null
	): Double {
		val d = widthH / heightH;
		val dd = d * d;

		yA *= d;
		yB *= d;

		val dX = xB - xA;
		val dY = yB - yA;
		val lAB = Math.sqrt(dX * dX + dY * dY);
		val xD = dX / lAB;
		val yD = dY / lAB;
		val a = (xC - xA) * xD + (yC - yA) * yD;
		val aa = a * a;
		val ee = xA * xA + yA * yA;
		val rr = widthH * widthH;
		val dR = rr - ee + aa;
		var intersectionCount = 0;

		if (dR >= 0.0) {
			val dT = Math.sqrt(dR);
			val sA = a - dT;
			val sB = a + dT;
			val inSideA = sA < 0.0 ? -1 : (sA <= lAB ? 0 : 1);
			val inSideB = sB < 0.0 ? -1 : (sB <= lAB ? 0 : 1);
			val sideAB = inSideA * inSideB;

			if (sideAB < 0) {
				return -1;
			}
			else if (sideAB === 0) {
				if (inSideA === -1) {
					intersectionCount = 2; // 10
					xB = xA + sB * xD;
					yB = (yA + sB * yD) / d;

					if (intersectionPointA !== null) {
						intersectionPointA.x = xB;
						intersectionPointA.y = yB;
					}

					if (intersectionPointB !== null) {
						intersectionPointB.x = xB;
						intersectionPointB.y = yB;
					}

					if (normalRadians !== null) {
						normalRadians.x = Math.atan2(yB / rr * dd, xB / rr);
						normalRadians.y = normalRadians.x + Math.PI;
					}
				}
				else if (inSideB === 1) {
					intersectionCount = 1; // 01
					xA = xA + sA * xD;
					yA = (yA + sA * yD) / d;

					if (intersectionPointA !== null) {
						intersectionPointA.x = xA;
						intersectionPointA.y = yA;
					}

					if (intersectionPointB !== null) {
						intersectionPointB.x = xA;
						intersectionPointB.y = yA;
					}

					if (normalRadians !== null) {
						normalRadians.x = Math.atan2(yA / rr * dd, xA / rr);
						normalRadians.y = normalRadians.x + Math.PI;
					}
				}
				else {
					intersectionCount = 3; // 11

					if (intersectionPointA !== null) {
						intersectionPointA.x = xA + sA * xD;
						intersectionPointA.y = (yA + sA * yD) / d;

						if (normalRadians !== null) {
							normalRadians.x = Math.atan2(intersectionPointA.y / rr * dd, intersectionPointA.x / rr);
						}
					}

					if (intersectionPointB !== null) {
						intersectionPointB.x = xA + sB * xD;
						intersectionPointB.y = (yA + sB * yD) / d;

						if (normalRadians !== null) {
							normalRadians.y = Math.atan2(intersectionPointB.y / rr * dd, intersectionPointB.x / rr);
						}
					}
				}
			}
		}

		return intersectionCount;
	}

	protected _onClear(): Unit {
		super._onClear();

		this.type = BoundingBoxType.Ellipse;
	}
	/**
	 * @inheritDoc
	 */
	public containsPoint(pX: Double, pY: Double): Boolean {
		val widthH = this.width * 0.5;
		if (pX >= -widthH && pX <= widthH) {
			val heightH = this.height * 0.5;
			if (pY >= -heightH && pY <= heightH) {
				pY *= widthH / heightH;
				return Math.sqrt(pX * pX + pY * pY) <= widthH;
			}
		}

		return false;
	}
	/**
	 * @inheritDoc
	 */
	public intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: { x: Double, y: Double }? = null,
		intersectionPointB: { x: Double, y: Double }? = null,
		normalRadians: { x: Double, y: Double }? = null
	): Double {
		val intersectionCount = EllipseBoundingBoxData.ellipseIntersectsSegment(
			xA, yA, xB, yB,
			0.0, 0.0, this.width * 0.5, this.height * 0.5,
			intersectionPointA, intersectionPointB, normalRadians
		);

		return intersectionCount;
	}
}
/**
 * - The polygon bounding box data.
 * @version DragonBones 5.1
 * @language en_US
 */
/**
 * - 多边形边界框数据。
 * @version DragonBones 5.1
 * @language zh_CN
 */
class PolygonBoundingBoxData  :  BoundingBoxData {
	public static toString(): String {
		return "[class dragonBones.PolygonBoundingBoxData]";
	}
	/**
	 * @private
	 */
	public static polygonIntersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		vertices:  DoubleArray,
		intersectionPointA: { x: Double, y: Double }? = null,
		intersectionPointB: { x: Double, y: Double }? = null,
		normalRadians: { x: Double, y: Double }? = null
	): Double {
		if (xA === xB) {
			xA = xB + 0.000001;
		}

		if (yA === yB) {
			yA = yB + 0.000001;
		}

		val count = vertices.length;
		val dXAB = xA - xB;
		val dYAB = yA - yB;
		val llAB = xA * yB - yA * xB;
		var intersectionCount = 0;
		var xC = vertices[count - 2];
		var yC = vertices[count - 1];
		var dMin = 0.0;
		var dMax = 0.0;
		var xMin = 0.0;
		var yMin = 0.0;
		var xMax = 0.0;
		var yMax = 0.0;

		for (var i = 0; i < count; i += 2) {
			val xD = vertices[i];
			val yD = vertices[i + 1];

			if (xC === xD) {
				xC = xD + 0.0001;
			}

			if (yC === yD) {
				yC = yD + 0.0001;
			}

			val dXCD = xC - xD;
			val dYCD = yC - yD;
			val llCD = xC * yD - yC * xD;
			val ll = dXAB * dYCD - dYAB * dXCD;
			val x = (llAB * dXCD - dXAB * llCD) / ll;

			if (((x >= xC && x <= xD) || (x >= xD && x <= xC)) && (dXAB === 0.0 || (x >= xA && x <= xB) || (x >= xB && x <= xA))) {
				val y = (llAB * dYCD - dYAB * llCD) / ll;
				if (((y >= yC && y <= yD) || (y >= yD && y <= yC)) && (dYAB === 0.0 || (y >= yA && y <= yB) || (y >= yB && y <= yA))) {
					if (intersectionPointB !== null) {
						var d = x - xA;
						if (d < 0.0) {
							d = -d;
						}

						if (intersectionCount === 0) {
							dMin = d;
							dMax = d;
							xMin = x;
							yMin = y;
							xMax = x;
							yMax = y;

							if (normalRadians !== null) {
								normalRadians.x = Math.atan2(yD - yC, xD - xC) - Math.PI * 0.5;
								normalRadians.y = normalRadians.x;
							}
						}
						else {
							if (d < dMin) {
								dMin = d;
								xMin = x;
								yMin = y;

								if (normalRadians !== null) {
									normalRadians.x = Math.atan2(yD - yC, xD - xC) - Math.PI * 0.5;
								}
							}

							if (d > dMax) {
								dMax = d;
								xMax = x;
								yMax = y;

								if (normalRadians !== null) {
									normalRadians.y = Math.atan2(yD - yC, xD - xC) - Math.PI * 0.5;
								}
							}
						}

						intersectionCount++;
					}
					else {
						xMin = x;
						yMin = y;
						xMax = x;
						yMax = y;
						intersectionCount++;

						if (normalRadians !== null) {
							normalRadians.x = Math.atan2(yD - yC, xD - xC) - Math.PI * 0.5;
							normalRadians.y = normalRadians.x;
						}
						break;
					}
				}
			}

			xC = xD;
			yC = yD;
		}

		if (intersectionCount === 1) {
			if (intersectionPointA !== null) {
				intersectionPointA.x = xMin;
				intersectionPointA.y = yMin;
			}

			if (intersectionPointB !== null) {
				intersectionPointB.x = xMin;
				intersectionPointB.y = yMin;
			}

			if (normalRadians !== null) {
				normalRadians.y = normalRadians.x + Math.PI;
			}
		}
		else if (intersectionCount > 1) {
			intersectionCount++;

			if (intersectionPointA !== null) {
				intersectionPointA.x = xMin;
				intersectionPointA.y = yMin;
			}

			if (intersectionPointB !== null) {
				intersectionPointB.x = xMax;
				intersectionPointB.y = yMax;
			}
		}

		return intersectionCount;
	}
	/**
	 * @private
	 */
	public x: Double;
	/**
	 * @private
	 */
	public y: Double;
	/**
	 * - The polygon vertices.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 多边形顶点。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	public val vertices:  DoubleArray = [];

	protected _onClear(): Unit {
		super._onClear();

		this.type = BoundingBoxType.Polygon;
		this.x = 0.0;
		this.y = 0.0;
		this.vertices.length = 0;
	}
	/**
	 * @inheritDoc
	 */
	public containsPoint(pX: Double, pY: Double): Boolean {
		var isInSide = false;
		if (pX >= this.x && pX <= this.width && pY >= this.y && pY <= this.height) {
			for (var i = 0, l = this.vertices.length, iP = l - 2; i < l; i += 2) {
				val yA = this.vertices[iP + 1];
				val yB = this.vertices[i + 1];
				if ((yB < pY && yA >= pY) || (yA < pY && yB >= pY)) {
					val xA = this.vertices[iP];
					val xB = this.vertices[i];
					if ((pY - yB) * (xA - xB) / (yA - yB) + xB < pX) {
						isInSide = !isInSide;
					}
				}

				iP = i;
			}
		}

		return isInSide;
	}
	/**
	 * @inheritDoc
	 */
	public intersectsSegment(
		xA: Double, yA: Double, xB: Double, yB: Double,
		intersectionPointA: { x: Double, y: Double }? = null,
		intersectionPointB: { x: Double, y: Double }? = null,
		normalRadians: { x: Double, y: Double }? = null
	): Double {
		var intersectionCount = 0;
		if (RectangleBoundingBoxData.rectangleIntersectsSegment(xA, yA, xB, yB, this.x, this.y, this.x + this.width, this.y + this.height, null, null, null) !== 0) {
			intersectionCount = PolygonBoundingBoxData.polygonIntersectsSegment(
				xA, yA, xB, yB,
				this.vertices,
				intersectionPointA, intersectionPointB, normalRadians
			);
		}

		return intersectionCount;
	}
}