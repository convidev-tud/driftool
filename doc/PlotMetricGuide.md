# Understanding the Drift Metric and Driftool Plot

## Drift Metric

The driftool calculates the *Drift* for two difference measures.

* **sd** is the *Statement Drift* := a measure for the merge complexity based on the pair-wise merge conflict count as a distance
* **dd** is the *Difference Drift* := a measure for the merge complexity based on the paire-wise difference count as a distance

In general, higher numbers indicate a more complex repository management. The most interesting value is the change of the drift metrics over time. Increases the drift rapidely, severe conflicts may have been introduced recently.

Using the above distance measures, the unit of drift is *Lines of Code (LoC)*. But we recommend to use drift as a unit-less measure.

### Calculation Details

1. We calculate the pairwise distances using a distance metric.
2. The pairwise distances are used to perform a multidimensional scaling of the set of branches to a 3D pointcloud.
3. We calculate the standard deviation of euclidean distances from the mean point.

For better understanding and transparency, we use the following standard deviation calculation method:
```python
def calculate_standard_deviation(embeddings: np.ndarray[float]) -> float:
    '''
    Input embeddings in the form [[x0, y0, z0], ..., [xi, yi, zi]]
    '''
    m = embeddings.mean(axis=0)
    l = len(embeddings)
    omega_squared = 0

    for p in embeddings:
        omega_squared += (((m[0] - p[0]) ** 2) + ((m[1] - p[1]) ** 2) + ((m[2] - p[2]) ** 2))
    
    omega_squared = omega_squared / l
    omega = math.sqrt(omega_squared)
    
    return omega
```

## Scatter Plot

As explained above in *Calculation Details*, we perform a multidimensional scaling (MDS) into 3D space.
Each point in the scatterplot visualizes one branch (the latest commit / HEAD of the branch)

The initial dimensionality of the distance (consistency) problem is unknown. The unit of the distance is LoC (using the driftool distance measures). The MDS tries to retain the pairwise distances while embedding the points in the target dimensions. The embedding error is called stress. A MDS algorithm tries to optimize for minimal stress.

**Consequences**

* Distances between points visualize the difference.
* The dimensions of the plot have no concrete unit or meaning, they are result of the MDS embedding.
* Point embeddings are not exact but approximated!
* Two points with a different locations do not neccessarily have a pair-wise difference, but different differences to a third point.

💡 **We recommend the scatter plot to get a visual overview of the project. Outlier detection is possible. However, the main indicator is the evolution of the drift metric over time and not size and strucutre of the scatter plot.**

---

> https://de.wikipedia.org/wiki/Varianz_(Stochastik)#Beziehung_zur_Standardabweichung

> https://en.wikipedia.org/wiki/Multidimensional_scaling

> https://math.stackexchange.com/questions/850228/finding-how-spreaded-a-point-cloud-in-3d
