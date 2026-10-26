# Feature equality checks

## 2d spatial equality check

To check if two `features` are equal:

1. Gather the `polygons` for each feature and
2. Union each collection into single EsriJson Polygons with
   the [unionOperator](https://developers.arcgis.com/javascript/latest/references/core/geometry/operators/unionOperator/)
3. Use the Arc GIS JS
   SDK [equalsOperator](https://developers.arcgis.com/javascript/latest/references/core/geometry/operators/equalsOperator/) to
   check if two polygons are topologically equal.

### What is topological equality?

Two polygons are topologically equal if their shape is the same and in the same location regardless of the coordinate order in the
JSON or generalization of the geometry.

**Examples:**

**The extra vertices in B lie on A's edges, so they change how the polygon is generalized but not the shape it covers:**

```
Geometry A (4 vertices)                     Geometry B (8 vertices, a midpoint on each edge)

  (0,2)          (2,2)                           (0,2)    (1,2)    (2,2)
    +--------------+                               +--------+--------+
    |              |                               |                 |
    |              |          ===           (0,1)  +                 +  (2,1)
    |              |                               |                 |
    +--------------+                               +--------+--------+
  (0,0)          (2,0)                           (0,0)    (1,0)    (2,0)

Result: equalsOperator.execute(A, B) returns TRUE
```

**The order of coordinates stored in the JSON are sorted clockwise vs anti-clockwise for the same square:**

```
Geometry A (Starts Top-Left, Clockwise)      Geometry B (Starts Bottom-Right, Counter-Clockwise)

(Start) 1 --------> 2                       3 <-------- 2
        |           |                       |           |
        |           |           ===         |           |
        |           |                       |           |
        4 <-------- 3                       4 --------> 1 (Start)

Result: equalsOperator.execute(A, B) returns TRUE
```

**Polygons of the same size at different locations:**

```
Geometry A (At Coordinates 0,0)          Geometry B (At Coordinates 10,10)
                                        
   (0,1) +-------+ (1,1)                           (10,11) +-------+ (11,11)
         |       |                                         |       |
         |       |                  =/=                    |       |
         |       |                                         |       |
   (0,0) +-------+ (1,0)                           (10,10) +-------+ (11,10)

Result: equalsOperator.execute(A, B) returns FALSE
```

## 3d spatial equality check

TODO LMS2-254: add documentation