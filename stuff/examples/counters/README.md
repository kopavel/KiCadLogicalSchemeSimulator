![Sequential counters with LED](img/counters.png)

**Sequential counters with LED, ideal for speed test.**

On an Intel i-3770s, this schema achieved approximately 690 MHz.

For comparison, [_Digital_](https://github.com/hneemann/Digital) on the same hardware achieved more than 100 times less speed.  
However, important to note that _Digital_ and this project have different schema building principles,
which may affect simulation speed.  
Additionally, this project provides real-time feedback on achieved frequency,
while _Digital_ doesn't offer this feature.
This leads to the necessity of using a clock for speed measurement in _Digital_, which is not very accurate.
![Digital](img/digital.png)

for start use counters gradle task

```
./gradlew conters
```
