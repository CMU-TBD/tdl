Task Control Management x.y.z (MON-DAY-YEAR)
Goal        run-auto,wait {1}:        TCM {0} --> ON HOLD         (Inactive)
Goal        run-auto,wait {1}:  ON HOLD  --> TCM             (Sent)
run()
Goal                  bar {2}:        TCM {1} --> ON HOLD         (Inactive)
Goal                  bar {2}:  ON HOLD  --> TCM             (Sent)
  Failure   run-auto,wait {1}:
foo(double)
Goal                  bar {3}:        TCM {2} --> ON HOLD         (Inactive)
Goal                  bar {3}:  ON HOLD  --> TCM             (Sent)
  Success             bar {2}:
foo(int)
Goal                  bar {4}:        TCM {3} --> ON HOLD         (Inactive)
Goal                  bar {4}:  ON HOLD  --> TCM             (Sent)
  Success             bar {3}:
foo()
  Success             bar {4}:
