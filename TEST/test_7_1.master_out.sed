Task Control Management x.y.z (MON-DAY-YEAR)
Goal       test-auto,wait {1}:        TCM {0} --> ON HOLD         (Inactive)
Goal       test-auto,wait {1}:  ON HOLD  --> TCM             (Sent)
_TDL_TerminateInTime ( Constrain=a-0 ,  Time= 0 : 0 : 2 . 0 )
Constraint:  _TDL_TerminateInTime  (0x........)
 Time= 0 : 0 : 2 . 0
 referenceInterval = Unknown Interval
 referenceState = Unknown State
 ActualReferenceNode = (nil)   ("NULL")

Monitor                 a {2}:        TCM {1} --> ON HOLD         (Inactive)
_TDL_TerminateInTime ( Constrain=b-0 ,  Time= 0 : 0 : 3 . 0 )
Constraint:  _TDL_TerminateInTime  (0x........)
 Time= 0 : 0 : 3 . 0
 referenceInterval = Unknown Interval
 referenceState = Unknown State
 ActualReferenceNode = (nil)   ("NULL")

Monitor                 b {4}:        TCM {1} --> ON HOLD         (Inactive)
_TDL_TerminateInTime ( Constrain=c-0 ,  Time= 0 : 0 : 4 . 0 )
Constraint:  _TDL_TerminateInTime  (0x........)
 Time= 0 : 0 : 4 . 0
 referenceInterval = Unknown Interval
 referenceState = Unknown State
 ActualReferenceNode = (nil)   ("NULL")

Monitor                 c {5}:        TCM {1} --> ON HOLD         (Inactive)
_TDL_ActivateImmediate ( Constrain=aTask )
Constraint:  _TDL_ActivateImmediate  (0x........)

_TDL_ActivateImmediate ( Constrain=aTask )
Constraint:  _TDL_ActivateImmediate  (0x........)

_TDL_ActivateImmediate ( Constrain=aTask )
Constraint:  _TDL_ActivateImmediate  (0x........)

_TDL_ActivateImmediate ( Constrain=aTask )
Constraint:  _TDL_ActivateImmediate  (0x........)

_TDL_ActivateImmediate ( Constrain=aTask )
Constraint:  _TDL_ActivateImmediate  (0x........)

_TDL_ActivateImmediate ( Constrain=aTask )
Constraint:  _TDL_ActivateImmediate  (0x........)

_TDL_ActivateAtEvent ( Constrain=aTask_next , Ref=aTask [Ref_flags: RUNNING] , RefInterval=**UNKNOWN_INTERVAL** , RefState=COMPLETED_STATE )
Constraint:  _TDL_ActivateAtEvent  (0x........)
 referenceInterval = Unknown Interval
 referenceState = Completed State
 ActualReferenceNode = 0x........   ("aTask")

_TDL_ActivateAtEvent ( Constrain=aTask_next , Ref=aTask [Ref_flags: RUNNING] , RefInterval=**UNKNOWN_INTERVAL** , RefState=COMPLETED_STATE )
Constraint:  _TDL_ActivateAtEvent  (0x........)
 referenceInterval = Unknown Interval
 referenceState = Completed State
 ActualReferenceNode = 0x........   ("aTask")

Monitor                 a {2}:  ON HOLD  --> TCM             (Sent)
Monitor                 b {4}:  ON HOLD  --> TCM             (Sent)
Monitor                 c {5}:  ON HOLD  --> TCM             (Sent)
  Success  test-auto,wait {1}:
Command             ACT-a {6}:        TCM {2} --> ON HOLD         (Inactive)
Command             ACT-a {7}:        TCM {2} --> ON HOLD         (Inactive)
Command             ACT-b {8}:        TCM {4} --> ON HOLD         (Inactive)
Command             ACT-b {9}:        TCM {4} --> ON HOLD         (Inactive)
Command             ACT-c {10}:        TCM {5} --> ON HOLD         (Inactive)
Command             ACT-c {11}:        TCM {5} --> ON HOLD         (Inactive)
Command             ACT-a {6}:  ON HOLD  --> TCM             (Sent)
Command             ACT-a {7}:  ON HOLD  --> TCM             (Sent)
Command             ACT-b {8}:  ON HOLD  --> TCM             (Sent)
Command             ACT-b {9}:  ON HOLD  --> TCM             (Sent)
Command             ACT-c {10}:  ON HOLD  --> TCM             (Sent)
Command             ACT-c {11}:  ON HOLD  --> TCM             (Sent)
Test:  a
  Success           ACT-a {6}:
Test:  a
  Success           ACT-a {7}:
Test:  b
  Success           ACT-b {8}:
Test:  b
  Success           ACT-b {9}:
Test:  c
  Success           ACT-c {10}:
Test:  c
  Success           ACT-c {11}:
Will Terminate a {2} when all references to it are released
Command             ACT-b {12}:        TCM {4} --> ON HOLD         (Inactive)
Terminated a {2}
Command             ACT-b {12}:  ON HOLD  --> TCM             (Sent)
Test:  b
  Success           ACT-b {12}:
Will Terminate b {4} when all references to it are released
Command             ACT-c {13}:        TCM {5} --> ON HOLD         (Inactive)
Terminated b {4}
Command             ACT-c {13}:  ON HOLD  --> TCM             (Sent)
Test:  c
  Success           ACT-c {13}:
Will Terminate c {5} when all references to it are released
Terminated c {5}
