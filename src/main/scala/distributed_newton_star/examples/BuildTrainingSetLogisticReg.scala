package distributed_newton_star.examples

import java.util.Scanner
import java.io._

object TestData {
  def main(args: Array[String]) {
    val fileName = "input/train-labels"
    val in = new Scanner(new File(fileName))
    // TODO Auto-generated method stub
    val inputImagePath = "input/train-images";
    val inputLabelPath = "input/train-labels";
    val outputPath = "input/train-labels-formatted";
    val hashMap = new Array[Integer](10)

    val inImage = new FileInputStream(inputImagePath)
    val inLabel = new FileInputStream(inputLabelPath)

    val outputFile = new PrintWriter(new FileOutputStream(outputPath))

    val magicNumberImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())
    val numberOfImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())
    val numberOfRows = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())
    val numberOfColumns = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read())

    val magicNumberLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read())
    val numberOfLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read())

    
    val numberOfPixels = numberOfRows * numberOfColumns

    for (i <- 0 until 20) { // numberOfImages
      if (i % 100 == 0) { System.out.println("Number of images extracted: " + i); }

      for (p <- 0 until numberOfPixels) {
        val gray = inImage.read();
        outputFile.print(gray + " ")
      }
      val label = inLabel.read()
      outputFile.println(if(label == 0) 0 else 1)
    }

    inImage.close() 
    inLabel.close() 
    outputFile.close()
  }
}