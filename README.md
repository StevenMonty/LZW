# LZW Compression

## Goal: To improve the Θ(n) runtime of the textbook implementation of LZW compression on large input files.

This is accomplished by reading in the file as a byte stream using ```BinaryStdIn.java```. The CodeBook is
made using a TernarySearchTree that has been modified for this application to provide nearly Θ(1) access time.

This implementation also allows the CodeBook to dynamically increase its size above 4096 entries, allowing larger
codewords, resulting in a higher compression ratio.

There is also an option to allow the user to reset the codebook once it has become full via a cmd line argument.
When decompressing a file, the algorithm can automatically detect if the codebook was in reset mode or not
during the compression operation, leaving no need for the ```r``` flag when decompressing. 

## Usage:

Compiling: ```javac LZWmod.java```


Compression: ```java LZWmod - <file.txt >compressedFile.lzw```

Compression (Codebook reset mode): ```java LZWmod - r <file.txt >compressedFile.lzw```

Decompression: ```java LZWmod + <compressedFile.lzw >file.txt```
