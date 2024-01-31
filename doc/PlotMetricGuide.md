# Understanding the Drift Metric and Driftool Plot

## Drift Metric

The driftool calculates the *Drift* measure **sd** (gamma).

* **sd** is the *Statement Drift* := a measure for the merge complexity based on the pair-wise git merge conflict count as a distance

In general, higher numbers indicate a more complex repository management. The most interesting value is the change of the drift metrics over time. Increases the drift rapidely, severe conflicts may have been introduced recently.

Using the above distance measures, the unit of drift is *Lines of Code (LoC)*. But we recommend to use drift as a unit-less measure and interpret its tendencies only.

### Calculation Details

1. We calculate the pairwise distances using a distance metric.
2. The pairwise distances are used to perform a multidimensional scaling of the set of branches to a 3D pointcloud.
3. We calculate the *mean absolute deviation around a central point* (mean of distances from the median) of the pointlcoud and their euclidean distances..

For better understanding and transparency, we use provide the core computation:
```python
def calculate_median_distance_avg(embeddings: np.ndarray[float]) -> float:
    '''
    Input embeddings in the form [[x0, y0, z0], ..., [xi, yi, zi]]
    '''
    m = np.median(embeddings, axis=0)
    l = len(embeddings)
    d = 0

    for p in embeddings:
        d += math.sqrt((p[0] - m[0])**2 + (p[1] - m[1])**2 + (p[2] - m[2])**2)
    
    return d / l
```

## Scatter Plot

As explained above in *Calculation Details*, we perform a multidimensional scaling (MDS) into 3D space.
Each point in the scatterplot visualizes one branch (the latest commit / HEAD of the branch)

The initial dimensionality of the distance (consistency) problem is unknown. 
The unit of the distance is LoC of merge conflicts. 
The MDS tries to retain the pairwise distances while embedding the points in the target dimensions. 
The embedding error is called stress. The MDS algorithm tries to optimize for minimal stress.

**Consequences**

* Distances between points visualize the amount of merge conflicts (inconsistencies).
* The dimensions of the plot have no relieable unit, they are result of the MDS embedding.
* Point embeddings are not exact but approximated!
* Two points with a different locations do not neccessarily have a pair-wise difference, but different differences to a third point.

💡 **We recommend the scatter plot to get a visual and explainable overview of the project. Outlier detection is possible. However, the main indicator is the evolution of the drift metric over time and not size and strucutre of the scatter plot.**

---

> https://en.wikipedia.org/wiki/Average_absolute_deviation

> https://en.wikipedia.org/wiki/Multidimensional_scaling
