## Keyboard

Matrix of key using host keyboard.

### Pins

#### Input names:

- `In[x]`- Row inputs.  
  x - number in range [0…7].
- `En`- Output enabled.  
  `Low` put outputs to Hi-Impedance

#### Output names:

- `Out[x]`- Column outputs.  
  x - number in range [0…7].
- `Ev`- event  
  Raising front signaling about events on a keyboard. Can be used for an interrupt.

### Parameters

#### Mandatory parameters:

- `map`- Key mapping to row/column.
  Key maps separated by `|`, single key map is <keyname>_<row><column>, like `A_03|B_23`

#### Optional parameters:

- none

### Example

Trs80-I keyboard:  
`map=A_01|B_02|C_03|D_04|E_05|F_06|G_07|H_10|I_11|J_12|K_13|L_14|M_15|N_16|O_17|P_20|Q_21|R_22|S_23|T_24|U_25|V_26|W_27|X_30|Y_31|Z_32|0_40|1_41|2_42|3_43|4_44|5_45|6_46|7_47|8_50|9_51|Semicolon_52|Quote_53|Comma_54|Minus_55|Period_56|Slash_57|Enter_60|Home_61|Escape_62|Up_63|Down_64|Left_65|Right_66|Space_67|Shift_70|Ctrl_74|Backspace_65|Back Quote_00` 

