# Parallel computing

Image denoising case study

## Quick start
1. Get the dataset of the images at the following locations (credits to [Jun Xu, Hui Li, Zhetong Liang, David Zhang, and Lei Zhang](https://github.com/csjunxu/PolyU-Real-World-Noisy-Images-Dataset)):
   * [download the full dataset of 40 images.](https://drive.google.com/open?id=1Q1auaG0Q5nF1OtI9R38gl6mTZj4LNCnX)
   * [download the small dataset of 10 images.](https://drive.google.com/open?id=14A-5QL8F2A7fe0dP-aVmvF7Yb3BtPIbt) (the small dataset is already included in the project)
   * [Optional] Place the downloaded zip in the root of this project directory and rename the zip to `image_dataset.zip`.
2. Run the following command to build the project
```bash
mvn install
```
5. Run the `server.jar` in the project directory with the following parameters `java -jar server.jar <hostname> <port number> <number of runs>`
6. Run the `client.jar` with the following parameters `java -jar client.jar <client id (a number)> <client output folder path> <server resource path> <server upload path>`
7. ???
8. Profit.