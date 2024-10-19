## ROM

ROM with a defined amount of address inputs and data outputs.

### Pins

#### Input names:

- `Ax`
  x - sequential number in range [0…aSize-1].
- `CS`- Chip Select.

#### Output names:

- `Dx`- data outputs.
  x - sequential number in range [2…size-1].

### Parameters

#### Mandatory parameters:

- `size`- amount of data pins in range [2…64].
- `aSize`- amount of address pins in range [1…32].
- `file`- path to the file, from which ROM content populated.
  Absolute or relative to working folder.
  The file content taken on a 'byte-by-byte' basis if size less than or equal to 8.  
  For size, greater than 8, the content is read in BigEndian order.

#### Optional parameters:

- `reverse`- input `CS` reversed.

### Example

4Kb 8-bit ROM, populated from the file `bios.bin`:  
`size=8;aSize=12;file=bios.bin`
