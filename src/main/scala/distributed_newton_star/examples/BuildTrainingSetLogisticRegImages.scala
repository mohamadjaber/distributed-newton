package distributed_newton_star.examples

import java.util.Scanner
import java.io._

import breeze.linalg._
import java.io.File
import util.Random._

object TestData {
  def main(args: Array[String]) {
    val fileName = "input/train-labels"
    val in = new Scanner(new File(fileName))
    // TODO Auto-generated method stub
    val inputImagePath = "input/train-images";
    val inputLabelPath = "input/train-labels";
    val outputPath = "input/train-labels-formatted";

    val inImage = new FileInputStream(inputImagePath)
    val inLabel = new FileInputStream(inputLabelPath)

    val outputFile = new PrintWriter(new FileOutputStream(outputPath))

    val magicNumberImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())
    var numberOfImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())

    val numberOfRows = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())
    val numberOfColumns = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())

    val magicNumberLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read())
    val numberOfLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read())

    val numberOfPixels = numberOfRows * numberOfColumns
    numberOfImages = 20

        
    val pictures: DenseMatrix[Double] = DenseMatrix.zeros(numberOfImages, numberOfPixels)
    val labels: DenseVector[Double] = DenseVector.zeros(numberOfImages)

    for (i <- 0 until numberOfImages) { // numberOfImages
      if (i % 100 == 0) { System.out.println("Number of images extracted: " + i); }

      for (p <- 0 until numberOfPixels) {
        val gray = inImage.read() / 255.0
        pictures(i, p) = gray
      }
      val label = inLabel.read()
      labels(i) = if (label == 0) 0 else 1
    }

    val comp = 50

    val picturesPCA = pca(pictures, comp)
    val maxValue = max(picturesPCA)
    for (p <- 0 until comp) {
      val avg = sum(picturesPCA(::, p)) / picturesPCA.rows
      for (i <- 0 until numberOfImages) {
        picturesPCA(i, p) = (picturesPCA(i, p) - avg) / maxValue
      }
    }

    for (i <- 0 until numberOfImages) {
      for (p <- 0 until comp) {
        outputFile.print(picturesPCA(i, p) + " ")
      }
      outputFile.println(labels(i))
    }

    inImage.close()
    inLabel.close()
    outputFile.close()
  }
  
  // to be verified with Haitham
  private def pca(data: DenseMatrix[Double], components: Int) = {
    val d = zeroMean(data.t)
    val v = svd(d.t)
    val model = v.Vt(::, 0 until components) // top 'components' Eigenvectors   
    val dd = model.t * d
    dd.t
  }

  private def mean(v: Vector[Double]) = (v.valuesIterator.sum) / v.size

  private def zeroMean(m: DenseMatrix[Double]) = {
    val copy = m.copy
    for (c <- 0 until m.cols) {
      val col = copy(::, c)
      val colMean = mean(col)
      col -= colMean
    }
    copy
  }

}