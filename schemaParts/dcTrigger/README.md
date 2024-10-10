## DC-trigger

DC-trigger with R and S inputs.

### Pins

#### Input names:

- `D`- data
- `C`- clock  
  sensible to raising edge.
- `R`- reset  
  active on `Hi`
- `S`- set  
  active on `Hi`

#### Output names:

- `Q`- output
- `~{Q}`- reverse output

### Parameters

#### Mandatory parameters:

- none

#### Optional parameters:

- `reverse`- `C` sensitive to falling edge.
- `setReverse`- `R` and `s` active on `Lo`.

### Example

DcTrigger with `R` and `S` active on `Lo`: `setReverse` 