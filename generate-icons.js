const fs = require('fs');
const path = require('path');

// Simple minimal PNG generator using pure JS (1x1 transparent scaled or valid PNG header)
// Creates a simple valid PNG for PWA icon placeholders if sharp is not installed
function createSimplePng(filePath, width, height, r, g, b) {
  // A minimal valid PNG buffer with solid indigo color
  const zlib = require('zlib');
  
  // PNG signature
  const signature = Buffer.from([137, 80, 78, 72, 13, 10, 26, 10]);
  
  // IHDR chunk
  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(width, 0);
  ihdrData.writeUInt32BE(height, 4);
  ihdrData[8] = 8; // 8 bit depth
  ihdrData[9] = 2; // RGB
  ihdrData[10] = 0; // deflate
  ihdrData[11] = 0; // filter
  ihdrData[12] = 0; // no interlace
  
  const ihdrChunk = createChunk('IHDR', ihdrData);
  
  // Raw scanlines: width * 3 bytes + 1 filter byte per line
  const rawData = Buffer.alloc(height * (width * 3 + 1));
  let offset = 0;
  for (let y = 0; y < height; y++) {
    rawData[offset++] = 0; // filter byte
    for (let x = 0; x < width; x++) {
      rawData[offset++] = r;
      rawData[offset++] = g;
      rawData[offset++] = b;
    }
  }
  
  const compressed = zlib.deflateSync(rawData);
  const idatChunk = createChunk('IDAT', compressed);
  const iendChunk = createChunk('IEND', Buffer.alloc(0));
  
  const png = Buffer.concat([signature, ihdrChunk, idatChunk, iendChunk]);
  fs.writeFileSync(filePath, png);
}

function createChunk(type, data) {
  const length = data.length;
  const chunk = Buffer.alloc(8 + length + 4);
  chunk.writeUInt32BE(length, 0);
  chunk.write(type, 4, 4, 'ascii');
  data.copy(chunk, 8);
  const crc = crc32(chunk.subarray(4, 8 + length));
  chunk.writeUInt32BE(crc, 8 + length);
  return chunk;
}

function crc32(buf) {
  let c = ~0;
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i];
    for (let k = 0; k < 8; k++) {
      c = (c >>> 1) ^ (-(c & 1) & 0xedb88320);
    }
  }
  return ~c >>> 0;
}

const iconsDir = path.join(__dirname, 'frontend', 'icons');
if (!fs.existsSync(iconsDir)) fs.mkdirSync(iconsDir, { recursive: true });

createSimplePng(path.join(iconsDir, 'icon-192.png'), 192, 192, 79, 70, 229);
createSimplePng(path.join(iconsDir, 'icon-512.png'), 512, 512, 79, 70, 229);
console.log('PNG Icons successfully generated.');

