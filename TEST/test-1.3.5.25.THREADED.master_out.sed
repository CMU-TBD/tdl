Task Control Management x.y.z (MON-DAY-YEAR)
Goal        A-outsideTask {1}:        TCM {0} --> ON HOLD         (Inactive)
Goal        A-outsideTask {1}:  ON HOLD  --> TCM             (Sent)
Goal A: Root Node   --  16384  --  main: 16384
Goal                    B {2}:        TCM {1} --> ON HOLD         (Inactive)
Goal          E-auto,wait {3}:        TCM {1} --> ON HOLD         (Inactive)
Goal                    B {2}:  ON HOLD  --> TCM             (Sent)
Goal          E-auto,wait {3}:  ON HOLD  --> TCM             (Sent)
Goal B: A-outsideTask   --  16384  --  main: 16384
Goal                    C {4}:        TCM {2} --> ON HOLD         (Inactive)
Goal                    C {4}:  ON HOLD  --> TCM             (Sent)
Goal E: A-outsideTask   --  16384  --  main: 16384
  Success     E-auto,wait {3}:
  Success   A-outsideTask {1}:
Goal C: B   --  16386  --  main: 16384
Resume B: A-outsideTask   --  16386  --  main: 0
Resume B:  UserTaskForThreadStack:  0x........ (B)   16386
Goal                    D {5}:        TCM {2} --> ON HOLD         (Inactive)
Goal          E-auto,wait {6}:        TCM {2} --> ON HOLD         (Inactive)
Goal                    D {5}:  ON HOLD  --> TCM             (Sent)
Goal          E-auto,wait {6}:  ON HOLD  --> TCM             (Sent)
Goal D: B   --  16386  --  main: 16386
Goal D:  UserTaskForThreadStack:  (nil) (nil)   16386
Goal D:  UserTaskForThreadStack:  0x........ (B)   16386
Goal          F-auto,wait {7}:        TCM {5} --> ON HOLD         (Inactive)
Goal          F-auto,wait {7}:  ON HOLD  --> TCM             (Sent)
Goal E: B   --  16386  --  main: 16386
Goal E:  UserTaskForThreadStack:  (nil) (nil)   16386
Goal E:  UserTaskForThreadStack:  0x........ (B)   16386
  Success     E-auto,wait {6}:
Goal F: D   --  16386  --  main: 16386
Goal F:  UserTaskForThreadStack:  (nil) (nil)   16386
Goal F:  UserTaskForThreadStack:  0x........ (B)   16386
  Success     F-auto,wait {7}:
Goal                    F {8}:        TCM {5} --> ON HOLD         (Inactive)
_TDL_Wait ( Constrain=F-0 )
Constraint:  _TDL_Wait  (0x........)

Goal                    F {8}:  ON HOLD  --> TCM             (Sent)
Goal F: D   --  16386  --  main: 16386
Goal F:  UserTaskForThreadStack:  (nil) (nil)   16386
Goal F:  UserTaskForThreadStack:  0x........ (B)   16386
  Success               F {8}:
_TDL_DisableForTime ( Constrain=F-1 , ConstrainInterval=**UNKNOWN_INTERVAL** ,  Time= 100  (MSecs) )
Constraint:  _TDL_DisableForTime  (0x........)
 NodeToConstrainInterval = Unknown Interval
 Time= 100  (MSecs)
 referenceInterval = Unknown Interval
 referenceState = Unknown State
 ActualReferenceNode = (nil)   ("NULL")

Goal                    F {9}:        TCM {5} --> ON HOLD         (Inactive)
Root Node {0} [uh|ag|pg|al]
   A-outsideTask {1} [hd|ag|pg|al]
      B {2} [hg|ag|pg|al]
         C {4} [hg|ag|pg|al]
         D {5} [hg|ag|pg|al]
            F-auto,wait {7} [hd|ad|pd|al]
            F {8} [hd|ad|pd|al]
            F {9} [uh|ua|up|al]
         E-auto,wait {6} [hd|ad|pd|al]
      E-auto,wait {3} [hd|ad|pd|al]
  Success               D {5}:
clearEventQueuesIfClearQueuesNotRunning:  Running in thread [16386].
  Success               B {2}:
END Goal C: B   --  16386  --  main: 0
clearEventQueuesIfClearQueuesNotRunning:  Running in thread [16386].
  Success               C {4}:
Goal                    F {9}:  ON HOLD  --> TCM             (Sent)
Goal F: D   --  16384  --  main: 16384
  Success               F {9}:
