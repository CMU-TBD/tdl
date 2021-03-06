
/* Big granddaddy of them all class.  Handles most of the workload.
 * Represents a Single Task Declaration, and all that entails.
 *
 * Copyright (c) 2008, Carnegie Mellon University
 *     This software is distributed under the terms of the 
 *     Simplified BSD License (see tdl/LICENSE.TXT)
 *
 */

import java.util.Enumeration;

public class DataTaskDefinition extends DataComponent
			     implements DataValidateCode,
					RunOnSubcomponentInterface
{
	/* Class Constants */
  public    final static int      TASK_WITHOUT_BODY     = 1;
  public    final static int      TASK_AFTER_OPEN_PAREN = 2001;

  public    final static int      TASK_TASK      = 0;
  public    final static int      GOAL_TASK      = 1;
  public    final static int      COMMAND_TASK   = 2;
  public    final static int      MONITOR_TASK   = 3;
  public    final static int      EXCEPTION_TASK = 4;
  public    final static int      HANDLER_TASK   = 5;
  public    final static int      RESUME_TASK    = 6;
  public    final static int      DEFAULT_TASK   = 0;
  public    final static int      INVALID_TASK   = -1;

  public    final static String   EXTERN              = "extern";
  public    final static String   PERSISTENT          = "PERSISTENT";
  public    final static String   DISTRIBUTED         = "DISTRIBUTED";
  public    final static String   THREADED            = "THREADED";
  public    final static String   STATIC              = "static";
  public    final static String   VIRTUAL             = "virtual";
  public    final static String[] TASK_LEADS 
					= { DataTaskDefinition.EXTERN,
					    DataTaskDefinition.PERSISTENT,
					    DataTaskDefinition.DISTRIBUTED,
					    DataTaskDefinition.THREADED,
					    DataTaskDefinition.STATIC,
					    DataTaskDefinition.VIRTUAL      };

  public    final static String   TASK_TYPE_INDEX     = "TaskType";
  public    final static String   TASK_NAME_INDEX     = "TaskName";
  public    final static String   OPEN_PAREN          = "(";
  public    final static String   CLOSE_PAREN         = ")";
  public    final static String   COMMA               = ",";
  public    final static String   SEMICOLON           = ";";
  public    final static String   WITH                = "WITH";
  public    final static String   COLON               = ":";
  public    final static String   HANDLES             = "HANDLES";
  public    final static String   HANDLES_INDEX       = "HandlesIndex";
  public    final static String   HANDLES_COMMA_INDEX = "HandlesCommaIndex";
  public    final static String   TASK_BODY_INDEX     = "TaskBodyIndex";


  protected final static String[][]
     TASK_TYPES        = { { "Task"                 },
			   { "Goal"                 },
			   { "Command"              },
			   { "Monitor"              },
			   { "Exception"            },
			   { "Exception", "Handler" },
			   { "Resume"               } };

  protected final static int[][]
     VALID_CONSTRAINTS = {	/* Task */
			    {
			     },

				/* Goal */
			    { DataConstraint.EXPAND_FIRST,
			      DataConstraint.DELAY_EXPANSION,
			      DataConstraint.EXCEPTION_HANDLER,
			      DataConstraint.ON_TERMINATE,
			      DataConstraint.DISTRIBUTED_FORMAT
			     },

				/* Command */
			    { DataConstraint.EXPAND_FIRST,
			      DataConstraint.DELAY_EXPANSION,
			      DataConstraint.EXCEPTION_HANDLER,
			      DataConstraint.ON_TERMINATE,
			      DataConstraint.DISTRIBUTED_FORMAT
			     },
			    
				/* Monitor */
			    { DataConstraint.EXPAND_FIRST,
			      DataConstraint.DELAY_EXPANSION,
			      DataConstraint.SEQUENTIAL_EXPANSION,
			      DataConstraint.SEQUENTIAL_EXECUTION,
			      DataConstraint.SERIAL,
			      DataConstraint.MAXIMUM_ACTIVATE,
			      DataConstraint.MAXIMUM_TRIGGER,
			      DataConstraint.MONITOR_PERIOD,
			      DataConstraint.EXCEPTION_HANDLER,
			      DataConstraint.ON_TERMINATE,
			      DataConstraint.DISTRIBUTED_FORMAT
			     },

				/* Exception */
			    {
			      DataConstraint.DISTRIBUTED_FORMAT
			     },

				/* Exception Handler */
			    {
			      DataConstraint.MAXIMUM_ACTIVATE,
			      DataConstraint.DISTRIBUTED_FORMAT
			     },

				/* Resume */
			    {
			     },
			  };

  protected final static boolean[]
    VALID_PERSISTENT_TASKS  = { false, true, true, true, false, false, false };
			     /* Task , Goal, COM., MON., Excep, Handl, Resume*/

  protected final static boolean[]
    VALID_DISTRIBUTED_TASKS = { false, true, true, true, true,  false, false };
			     /* Task , Goal, COM., MON., Excep, Handl, Resume*/

  protected final static boolean[]
    VALID_THREADED_TASKS    = { false, true, true, true, false, false, false };
			     /* Task , Goal, COM., MON., Excep, Handl, Resume*/

  protected final static boolean[]
    VALID_STATIC_TASKS      = { false, true, true, true, true,  true,  true  };
			     /* Task , Goal, COM., MON., Excep, Handl, Resume*/

  protected final static boolean[]
    VALID_VIRTUAL_TASKS     = { false, true, true, true, false, true,  true  };
			     /* Task , Goal, COM., MON., Excep, Handl, Resume*/




	/* Class Variables */
  protected static Registry   taskRegistry = new Registry ( true /*1-to-N*/ );


	/* Class Methods */
  protected static Registry getTaskRegistry() { return taskRegistry;}

  public static boolean unregisterTask (
				     DataTaskDefinition theDataTaskDefinition )
  {
    String    ourTaskName = theDataTaskDefinition . getTaskName();

	/* ALWAYS: IF we are registered, try to unregister */
    if ( getTaskRegistry() . getIsObjectRegistered ( theDataTaskDefinition ) )
    {
      if ( getTaskRegistry() . unregister ( theDataTaskDefinition ) == false )
      {
	/* Lets be verbose & warn of any potential problems... */
	System.err.println ( "[DataTaskDefinition:unregisterTask] Warning:  "
			     + "Task was unregistered...  (\""
			     + ourTaskName + "\")" );
	return false;
      }
    }

    return true;
  }


  public static boolean registerTask( DataTaskDefinition theDataTaskDefinition,
				      boolean            theThrowExceptions )
    throws DetailedParseException
  {
    String errorString;
    String ourTaskName = theDataTaskDefinition . getTaskScopeAndName();

	/* Trivial case -- don't register these... */
    if ( DataComponent.isEmptyString ( ourTaskName ) )
      return true;

	/* Note:  We now permit multiple tasks to be registered under the
	 * same name.  However:  Overloading of exception tasks is prohibited.
	 * The the task-types must match.  And the Distributed/Non-Distributed
	 * nature must match.
	 *
	 * Caveat:  This functionality occurs in two places.  Here and in
	 * TDLParser.parseTaskName(DataTaskDefinition).  The code
	 * here is invoked during the task-registration process, which
	 * currently occurs in DataFile after the entire TDL file has been
	 * read and parsed.
	 *
	 * This is the code that IS ACTUALLY UTILIZED.
	 *
	 * Note: If it doesn't match the first one, we won't register it.
	 *       Therefore, we only need to check the first one...
	 */

	/* Is this task name already registered? */
    if ( getFirstTaskForName ( ourTaskName ) != null )
    {
	/* Check for overloaded exception tasks */
      if (   (    getFirstTaskForName ( ourTaskName ) . getTaskType()
	       == DataTaskDefinition.EXCEPTION_TASK                   )
	  || (    theDataTaskDefinition               . getTaskType()
	       == DataTaskDefinition.EXCEPTION_TASK                   ) )
      {
	errorString
	  = theDataTaskDefinition . getMessageFilenameLead()
	  + theDataTaskDefinition . getLineNumberString()
	  + ":  [DataTaskDefinition:registerTask] Warning:  "
	  + "Task Registration Failure:  "
	  + "Task \"" + ourTaskName + "\" is both overloaded and of type "
	  + "\"Exception\".  Overloading is not permitted for "
	  + "\"Exception\" tasks.  "
	  + "Please choose another name for this task.";

	if ( theThrowExceptions )
	  throw new DetailedParseException ( errorString );
	else
	  System.err.println ( errorString );
	return false;
      }


	/* Check for Task-Type disagreement */
      if (    getFirstTaskForName ( ourTaskName ) . getTaskType()
	   != theDataTaskDefinition               . getTaskType() )
      {
	errorString
	  = theDataTaskDefinition . getMessageFilenameLead()
	  + theDataTaskDefinition . getLineNumberString()
	  + ":  [DataTaskDefinition:registerTask] Warning:  "
	  + "Task Registration Failure:  "
	  + "Task \"" + ourTaskName + "\" is of type \""
	  + theDataTaskDefinition               . getTaskTypeString()
	  + "\".  Task \"" + ourTaskName
	  + "\" is overloaded, and was previously declared (registered) under "
	  + "type \""
	  + getFirstTaskForName ( ourTaskName ) . getTaskTypeString()
	  + "\".  These Task-Types are supposed to be the same, as one of "
	  + "these Tasks will wind up being allocated with the other's "
	  + "Task-Type, creating issues with constraint satisfaction.  "
	  + "Please choose another name for this task, "
	  + "or make both of them the same Task-Type.";

	if ( theThrowExceptions )
	  throw new DetailedParseException ( errorString );
	else
	  System.err.println ( errorString );
	return false;
      }

	/* Check for Distributed/Non-Distributed disagreement. */
      if (    getFirstTaskForName ( ourTaskName ) . getIsDistributed()
	   != theDataTaskDefinition               . getIsDistributed() )
      {
	errorString
	  = theDataTaskDefinition . getMessageFilenameLead()
	  + theDataTaskDefinition . getLineNumberString()
	  + ":  [DataTaskDefinition:registerTask] Warning:  "
	  + "Task Registration Failure:  "
	  + "Task \"" + ourTaskName + "\" is \""
	  + ( (theDataTaskDefinition               . getIsDistributed())
	     ? "" : "NON-" )
	  + "DISTRIBUTED\".  Task \"" + ourTaskName
	  + "\" is overloaded, and was previously declared (registered) as \""
	  + ( (getFirstTaskForName ( ourTaskName ) . getIsDistributed())
	     ? "" : "NON-" )
	  + "DISTRIBUTED\".  This DISTRIBUTED nature must be the "
	  + "same between overloaded tasks, as one of these Tasks will wind "
	  + "up being allocated with the other's DISTRIBUTED/NON-DISTRIBUTED "
	  + "allocation function, with the associated problems.  "
	  + "Please choose another name for this task, make both of them "
	  + "\"DISTRIBUTED\", or make both of them \"NON-DISTRIBUTED\".";

	if ( theThrowExceptions )
	  throw new DetailedParseException ( errorString );
	else
	  System.err.println ( errorString );
	return false;
      }
    }

	/* Return the results of our attempt to register. */
    if (   getTaskRegistry() . register ( ourTaskName, theDataTaskDefinition )
	== false )
    {
      System.err.println ( "[DataTaskDefinition:registerTask] Warning:  "
			   + "Task registration failed..  (\""
			   + ourTaskName + "\")" );
      return false;
    }
    else
      return true;
  }

  public static DataVector getTasksForName ( String theName )
  {
    return getTaskRegistry() . getObjectsForName ( theName );
  }

  public static DataTaskDefinition getFirstTaskForName ( String theName )
  {
    return ( (DataTaskDefinition)
	     (getTaskRegistry() . getFirstObjectForName ( theName )) );
  }

  public static int getNumberOfTasksForName ( String theName )
  {
    return getTaskRegistry() . getNumberOfObjectsForName ( theName );
  }


  public static boolean getIsTaskRegistered (
				     DataTaskDefinition theDataTaskDefinition )
  {
    return getTaskRegistry() . getIsObjectRegistered ( theDataTaskDefinition );
  }

  public static Enumeration getRegisteredTasks ( )
  {
    return getTaskRegistry() . getObjects();
  }


  public static String getTaskTypeString ( int theTaskType )
  {
    if ( ( theTaskType < 0 )  || ( theTaskType > TASK_TYPES.length ) )
    {
      throw new CompilationException (
	 "[DataTaskDefiniton:getTaskTypeString]  Error:  Invalid Task-Type ("
	 + theTaskType + ")" );
    }

    String  taskTypeStrings[] = DataTaskDefinition.TASK_TYPES [ theTaskType ];
    String  aString = taskTypeStrings [ 0 ];

    for ( int i = 1;   i < taskTypeStrings.length;   i++ )
    {
      aString = aString + " " + taskTypeStrings [ i ];
    }

    return aString;
  }





	/* Instance Data */
  protected DataVector             taskLeads; /* ( extern,      persistent,
						   distributed, threaded   ) */
  protected int                    taskType;
  protected boolean                wasExplicitlyScoped;
  protected DataScope              taskScope;
  protected String                 taskName;
  protected DataVector             taskArguments;
  protected DataVector             persistentTaskDeclarations;
  protected DataVector             constraints;
  protected boolean                hasWithKeyword;
  protected DataCompoundStatement  taskBody;

	/* Exception tasks can have a base-class.  The DataSpawnTask class */
	/* has all the features we need to support this base-class (here). */
  protected DataSpawnTask          exceptionBaseTask;

	/* Exception Handler Tasks must have a "handles" clause, */
	/* specifying which exception they handle.               */
  protected String                 handlesException;

	/* Postpone'd tasks may have a second counterpart resume task */
	/* (Which, while not being a full-fledged Task,               */
	/*  is represented by a second DataTaskDefinition object.)    */
  protected DataVector             resumeTasksVector;

	/* And, for convenience, the link back from the RESUME to it's *
	 * corresponding "MASTER" task.                                */
  protected DataTaskDefinition     resumeMasterTask;


	/* statementsVector is created / returned by                  *
	 *  generateStatementsVector() & getCachedStatementsVector(). */
  protected DataVector             statementsVector;

    /*  __nonUniqueNamesVector is returned by validateTaskForCxxGeneration() */
  protected DataVector             __nonUniqueNamesVector;

	/* __onAgentHashtable is returned by getCachedOnAgentHashtable, *
	 * and is generated inside validateTaskForCxxGeneration().      */
  protected DataHashtable          __onAgentHashtable;

	/* These are used internally to validateTaskForCxxGeneration */
  protected DataVector             __taskRefsInWiths;
  protected DataHashtable          __labelHashtable;
  protected DataHashtable          __spawnHashtable;
  protected DataHashtable          __withsToComponents;

	/* Used for task-name overloading... */
	/* DataTaskDefinition sets it to a unique semi-random number. */
	/* DataFile re-sets it to the "_<filename>_<index>" string.   */
  protected String                 uniqueIdString;

	/* Instance Methods */
  public DataTaskDefinition ( )
  {
    this ( false, DataTaskDefinition.TASK_TASK, DataComponent.EMPTY_STRING );
  }

  public DataTaskDefinition ( boolean  theIsExtern,
			      int      theTaskType,
			      String   theTaskName )
  {
    taskLeads = new DataVector(3);
    setIsExtern ( theIsExtern );
    setTaskType ( theTaskType );

    setWasExplicitlyScoped();
    taskScope = new DataScope();
    taskName  = "";
    try { setTaskName ( theTaskName ); }
    catch ( DetailedParseException theException )
    {
      System.err.println ( "[DataTaskDefinition:DataTaskDefinition]  Warning: "
			   + " theTaskName is not parseable.  \"" 
			   + theTaskName + "\"  --  "
			   + theException.toString() );
    }

    taskArguments              = new DataVector();
    persistentTaskDeclarations = new DataVector();
    constraints                = new DataVector();
    hasWithKeyword             = false;
    taskBody                   = null;
    exceptionBaseTask          = null;
    handlesException           = null;
    resumeTasksVector          = new DataVector();
    resumeMasterTask           = null;
    statementsVector           = new DataVector();
    __nonUniqueNamesVector     = new DataVector();
    __onAgentHashtable         = new DataHashtable();
    __taskRefsInWiths          = new DataVector();
    __labelHashtable           = new DataHashtable();
    __spawnHashtable           = new DataHashtable();
    __withsToComponents        = new DataHashtable();

       /* Value re-set by DataFile to provide for more reasonable unique-ids.*/
    uniqueIdString             = getUniqueIdentifierString ( "_" );
  }


      /* Convenience Method -- This should *ALMOST* never need to be called.*/
  public boolean registerTask ( String  theErrorLocation,
				boolean theThrowExceptions )
    throws DetailedParseException
  {
    if ( DataTaskDefinition.registerTask ( this, theThrowExceptions ) == false)
    {
      System.err.println ( "[DataTaskDefinition:registerTask] --  "
			   + "[\"" + theErrorLocation + "\"] --  "
			   + "Error:  New Registration of task has failed. (\""
			   + getTaskScopeAndName() + "\").");
      return false;
    }
    else
      return true;
  }
      /* Convenience Method -- This should *ALMOST* never need to be called.*/
  public boolean unregisterTask ( String theErrorLocation )
  {
    if ( DataTaskDefinition.unregisterTask ( this ) == false )
    {
      System.err.println ( "[DataTaskDefinition:unregisterTask] --  "
		     + "[\"" + theErrorLocation + "\"] --  "
		     + "Warning:  unregistration of task has failed.  (\""
		     + getTaskScopeAndName() + "\")." );
      return false;
    }
    else
      return true;
  }
      /* Convenience Method */
  public boolean getIsTaskRegistered()
  {
    return DataTaskDefinition.getIsTaskRegistered ( this );
  }



	/* Task Leads.  Specifically:  extern, persistent, distributed. */
  protected DataVector getTaskLeads() { return taskLeads; }

  public boolean getIsExtern ()
    { return getTaskLeads() . contains ( DataTaskDefinition.EXTERN      ); }

  public boolean getIsPersistent ()
    { return getTaskLeads() . contains ( DataTaskDefinition.PERSISTENT  ); }

  public boolean getIsDistributed ()
    { return getTaskLeads() . contains ( DataTaskDefinition.DISTRIBUTED ); }

  public boolean getIsThreaded ()
    { return getTaskLeads() . contains ( DataTaskDefinition.THREADED    ); }

  public boolean getIsStatic ()
    { return getTaskLeads() . contains ( DataTaskDefinition.STATIC      ); }

  public boolean getIsVirtual ()
    { return getTaskLeads() . contains ( DataTaskDefinition.VIRTUAL     ); }

  public boolean getIsKeyIndex ( String theKeyIndex )
    { return getTaskLeads() . contains ( theKeyIndex                    ); }
 


  public boolean isValidTaskLead ( Object theString )
  {
    if ( /*NOT*/! ( theString instanceof String ) )
      return false;

    for ( int i = 0;  i < DataTaskDefinition.TASK_LEADS.length;  i++ )
      if ( theString . equals ( DataTaskDefinition.TASK_LEADS [ i ] ) )
	return true;

    return false;
  }

  public boolean hasTaskLeads()
    { return getTaskLeads() . size() > 0; }



  public boolean addExternKeywordToTaskLeadsOrder ()
  {
    if ( getIsExtern() )
      return false;
    getTaskLeads() . addElement ( DataTaskDefinition.EXTERN ); 
    return true;
  }

  public boolean addToTaskLeadsKeywordOrder ( String theKeyIndex )
    throws CompilationException
  {
    if ( /*NOT*/! isValidTaskLead ( theKeyIndex ) )
    {
      throw new CompilationException (
		     getMessageFilenameLead() + getLineNumberString()
		   + ":  [DataTaskDefintion:addToTaskLeadsKeywordOrder]  "
		   + "INTERNAL Error:  Invalid value for theKeyIndex (\""
		   + theKeyIndex + "\"" );
    }

    if ( getIsKeyIndex ( theKeyIndex ) )
      return false;

    getTaskLeads() . addElement ( theKeyIndex );
    return true;
  }




	/* Backward compatibility... */
  public void setIsExtern ( boolean theIsExtern )
  {
    if ( theIsExtern == true )
    {
      if ( getIsExtern() == false )
	addExternKeywordToTaskLeadsOrder();
    }
    else
    {
      while ( getIsExtern() == true )
      {
	getTaskLeads() . removeElement ( DataTaskDefinition.EXTERN );
      }
    }
  }



	/* Method dealing with DISTRIBUTED Task's formats! */
  public void writeDistributedMacroRequirements (
					DataDestination  theOutputDestination )
  {
    DataTaskArgument.writeDistributedMacroRequirements( theOutputDestination,
							getTaskArguments(),
							true );
  }

	/* Method dealing with DISTRIBUTED Task's formats! */
  public String getDistributedTaskFormatString ( int theSubsetToProduce )
    throws CompilationException
  {
    DataConstraint  distributedFormatConstraint;

    if ( getIsDistributed() == false )
    {
      System.err.println (
	"[DataTaskDefinition:getDistributedTaskFormat()]  "
	+ "Internal Error:  Invoked on non-distributed Task." );
      return null;
    }

    distributedFormatConstraint
      = findConstraint ( DataConstraint.DISTRIBUTED_FORMAT );

    if ( distributedFormatConstraint != null )
    {
      return
	distributedFormatConstraint . getDistributedFormatStringExpression()
	. toString ( theSubsetToProduce );
    }

    return DataTaskArgument.getDistributedFormatString (
	     getTaskArguments(),
	     this,
	     "task \"" + getTaskScopeAndName() + "\"",
	     "\"{\"",
	     "\"}\"" );
  }




  public int  getTaskType ( ) { return taskType; }
  public void setTaskType ( int theTaskType )
  {
    if (   ( theTaskType != DataTaskDefinition.TASK_TASK      )
	&& ( theTaskType != DataTaskDefinition.GOAL_TASK      )
	&& ( theTaskType != DataTaskDefinition.COMMAND_TASK   )
	&& ( theTaskType != DataTaskDefinition.MONITOR_TASK   )
	&& ( theTaskType != DataTaskDefinition.EXCEPTION_TASK )
	&& ( theTaskType != DataTaskDefinition.HANDLER_TASK   )
	&& ( theTaskType != DataTaskDefinition.RESUME_TASK    ) )
    {
      System.err.println ( "[DataTaskDefinition:setTaskType]  Error:  "
			   + "Invalid Task Type (" + theTaskType
			   + ").  Assuming default." );
      taskType = DataTaskDefinition.DEFAULT_TASK;
    }
    else
    {
      taskType = theTaskType;
    }
    validateAllConstraints ( true, true );
  }


  public String [] getTaskTypeStrings()
  {
    return DataTaskDefinition.TASK_TYPES [ getTaskType() ];
  }

  public String getTaskTypeString ( )
  {
    return DataTaskDefinition.getTaskTypeString ( getTaskType() );
  }



	/* In TDLParser.jj, scoping can be set manually during
	 * parseTaskScopeAndName(), or automatically, in parseFile(),
	 * via being inside a class/struct/namespace.
	 *
	 * Note: Does not indicate whether or not any scoping was applied.
	 * Use hasTaskScope() for that...
	 */
  public void    setWasExplicitlyScoped(){        wasExplicitlyScoped = true; }
  public boolean getWasExplicitlyScoped(){ return wasExplicitlyScoped;        }

  public void    setWasImplicitlyScoped(){        wasExplicitlyScoped = false;}
  public boolean getWasImplicitlyScoped(){ return wasExplicitlyScoped== false;}


  /*da0g: Handle scope - With - Parsing/Registration later on when/if needed.*/

  public DataScope  getTaskScope() { return taskScope; }

  public String getTaskScopeAndName()
  {
    return getTaskScope().getAllScopeStrings() + getTaskName();
  }


  public String  getTaskName ( ) { return taskName; }

  protected void setTaskNameWithoutParsingOrRegistration ( String theTaskName )
  {
	/* Special case for blank/empty name */
    if ( DataComponent . isEmptyString ( theTaskName ) )
      theTaskName = DataComponent.EMPTY_STRING;

	/* This belongs here for safety.  ALWAYS try to unregister! */
    unregisterTask ( "setTaskNameWithoutParsingOrRegistration" );

	/* Set the new task name */
    taskName = theTaskName;
  }


  protected boolean setTaskNameWithoutParsing ( String theTaskName )
    throws DetailedParseException /* Well, not really... From registerTask*/
  {
    String   oldTaskName = getTaskName();

	/* Special case for blank/empty name */
    if ( DataComponent . isEmptyString ( theTaskName ) )
      theTaskName = DataComponent.EMPTY_STRING;

	/* Note:  We now permit multiple tasks   *
	 * to be registered under the same name. */

	/* Set the task name (which will unregister us) */
    setTaskNameWithoutParsingOrRegistration ( theTaskName );

	/* Re-register this task... */
    if ( registerTask ( "setTaskNameWithoutParsing", false ) == false )
    {
      System.err.println ( "[DataTaskDefinition:setTaskNameWithoutParsing]  "
			   + "Warning:  Trying to restore old task name. [\""
			   + oldTaskName + "\"]  (And then failing...)" );

	/* Try to restore original name */
      setTaskNameWithoutParsingOrRegistration ( oldTaskName );

	  /* Try to reregister ourself back to where we were... */
      if ( registerTask ( "setTaskNameWithoutParsing", false ) == false )
      {
	System.err.println ( "[DataTaskDefinition:setTaskNameWithoutParsing]  "
		     + "Warning:  Attempt to re-register under restored "
		     + "task name (\"" + getTaskName()
		     + "\") has failed...  Failure is complete..." );
      }

      return false;
    }
    else
      return true;
  
}

  public boolean setTaskName ( String theTaskName )
    throws DetailedParseException
  {
	/* Trivial case */
    if ( getTaskName() . equals ( theTaskName ) )
      return true;

	/* Special case blank/empty name */
    if ( DataComponent . isEmptyString ( theTaskName ) )
      return setTaskNameWithoutParsing ( DataComponent.EMPTY_STRING );

	/* General case -- parse the name */
    TDLParser . reinitParser ( theTaskName );
    try
    {
	/* Use of hasScope() is a kludge...
	 * But this method -- setTaskName -- is only invoked from
	 * the generic DataTaskDefinition constructor.
	 */
      return setTaskNameWithoutParsing (
        TDLParser . getParser()
	          . parseTaskScopeAndName( null,
					   getTaskScope().hasScope() )
	          . image );
    }
    catch ( Throwable  theExceptionOrError )
    {
      didParseOfSubpartFail ( theExceptionOrError );
      return false;
    }
  }
  

  protected DataVector getConstraints()      {return constraints; }
  protected int        getConstraintsCount() {return getConstraints().count();}

  protected DataConstraint getConstraint ( int theIndex )
  {
    if ( ( theIndex < 0 )  ||  ( theIndex >= getConstraintsCount() ) )
    {
      System.err.println ( "[DataTaskDefinition:getConstraint]  Warning:  "
			   + "Invalid index (" + theIndex + ").  ["
			   + getConstraints().count() + "]" );
      return null;
    }

    return (DataConstraint) ( getConstraints().elementAt ( theIndex ) );
  }

  protected int findConstraintIndex ( int theConstraintType )
  {
    for ( int i = getConstraintsCount() - 1;  i >= 0;  i-- )
    {
      if ( getConstraint ( i ) . getConstraintType() == theConstraintType )
      {
	return i;
      }
    }
    return DataComponent.INVALID_INDEX;
  }

  protected DataConstraint findConstraint ( int theConstraintType )
  {
    int  constraintIndex = findConstraintIndex ( theConstraintType );

    if ( constraintIndex != DataComponent.INVALID_INDEX )
      return getConstraint ( constraintIndex );
    else
      return null;
  }

  protected boolean getHasConstraint ( int theConstraintType )
  {
    return  (    findConstraintIndex ( theConstraintType )
	      != DataComponent.INVALID_INDEX );
  }

  public boolean isValidConstraintType ( int theConstraintType )
  {
    int[]  validConstraints
		= DataTaskDefinition.VALID_CONSTRAINTS [ getTaskType() ];

    for ( int i=0;  i < validConstraints.length;  i++ )
    {
      if ( theConstraintType == validConstraints [ i ] )
      {
	return true;
      }
    }

    return false;
  }


  public boolean validateAllConstraints ( boolean theRemoveInvalidConstraints,
					  boolean theIsVerbose )
  {
    boolean returnValue = true;

    if ( getConstraints() == null )
      return returnValue;

    for ( int i=0;  i < getConstraintsCount();  i++ )
    {
      if ( isValidConstraintType ( getConstraint ( i ) . getConstraintType() )
	   == false )
      {
	returnValue = false;

	if ( theIsVerbose )
	{
	  System.err.println ( "[DataTaskDefinition:validateAllConstraints]  "
			       + "Warning:  Invalid Constraint-Type ( "
			       + getConstraint ( i ) . getConstraintType()
			       + " )." );
	}
	if ( theRemoveInvalidConstraints )
	{
	  removeConstraint ( i );
	  i--;
	}
      }
    }

    return returnValue;
  }


  public void addConstraint ( DataConstraint  theConstraint )
  {
    if ( isValidConstraintType ( theConstraint . getConstraintType() ) )
    {
      theConstraint . setParent ( this );
      getConstraints() . addElement ( theConstraint );
    }
    else
    {
      System.err.println ( "[DataTaskDefinition:addConstraint]  Warning:  "
			   + "Invalid Constraint-Type ( "
			   + theConstraint . getConstraintType() + " )." );
    }
  }

  public DataConstraint removeConstraint ( int  theIndex )
  {
    DataConstraint  returnObject;

    if ( (theIndex >= 0)  &&  (theIndex < getConstraintsCount()) )
    {
      returnObject =  ( (DataConstraint)
		       ( getConstraints() . removeElementAt ( theIndex ) ) );
      returnObject . setParent ( null );
      return returnObject;
    }
    else
    {
      System.err.println( "[DataTaskDefinition:removeConstraint]  Warning:  "
			  + "Invalid theIndex ( " + theIndex + " )." );
      return null;
    }
  }



	/* Methods to deal with Task Arguments */ 
  protected DataVector getTaskArguments()     { return taskArguments; }
  public    int        getTaskArgumentCount() { return getTaskArguments()
						         . count(); }

  public DataTaskArgument getTaskArgument ( int theIndex )
  {
    if ( (theIndex >= 0)  &&  (theIndex < getTaskArgumentCount()) )
      return (DataTaskArgument) (getTaskArguments() . elementAt ( theIndex ) );
    else
    {
      System.err.println ( "[DataTaskDefinition:getTaskArgument]  Warning:  "
			   + "Invalid theIndex ( " + theIndex + " )." );
      return null;
    }
  }

  public void addTaskArgument ( DataTaskArgument theDataTaskArgument )
  {
    theDataTaskArgument . setParent ( this );
    getTaskArguments() . addElement ( theDataTaskArgument );
  }

  public DataTaskArgument removeTaskArgument ( int theIndex )
  {
    DataTaskArgument  returnObject;

    if ( (theIndex >= 0)  &&  (theIndex < getTaskArgumentCount()) )
    {
      returnObject =  ( (DataTaskArgument)
		       ( getTaskArguments() . removeElementAt ( theIndex ) ) );
      returnObject . setParent ( null );
      return returnObject;
    }
    else
    {
      System.err.println( "[DataTaskDefinition:removeTaskArgument]  Warning:  "
			  + "Invalid theIndex ( " + theIndex + " )." );
      return null;
    }
  }



	/* Methods to deal with Persistent Task Declarations */ 
  protected DataVector getPersistentTaskDeclarations ( )
					 { return persistentTaskDeclarations; }

  public int getPersistentTaskDeclarationCount ( )
			  { return getPersistentTaskDeclarations() . count(); }

  public DataTaskArgument getPersistentTaskDeclaration ( int theIndex )
  {
    if ( (theIndex >= 0) && (theIndex < getPersistentTaskDeclarationCount()) )
      return ( (DataTaskArgument)
	       ( getPersistentTaskDeclarations() . elementAt ( theIndex ) ) );
    else
    {
      System.err.println( "[DataTaskDefinition:getPersistentTaskDeclaration]  "
		        + "Warning:  Invalid theIndex ( " + theIndex + " )." );
      return null;
    }
  }

  public void addPersistentTaskDeclaration (
					 DataTaskArgument theDataTaskArgument )
  {
    theDataTaskArgument . setParent ( this );
    getPersistentTaskDeclarations() . addElement ( theDataTaskArgument );
  }

  public DataTaskArgument removePersistentTaskDeclaration ( int theIndex )
  {
    DataTaskArgument  returnObject;

    if ( (theIndex >= 0) && (theIndex < getPersistentTaskDeclarationCount()) )
    {
      returnObject
	=  ( (DataTaskArgument)
	     ( getPersistentTaskDeclarations() . removeElementAt ( theIndex ) ) );
      returnObject . setParent ( null );
      return returnObject;
    }
    else
    {
      System.err.println("[DataTaskDefinition:removePersistentTaskDeclaration]"
		      + "  Warning:  Invalid theIndex ( " + theIndex + " )." );
      return null;
    }
  }




  public boolean  getHasWithKeyword()  { return hasWithKeyword; }
  public void     setHasWithKeyword( boolean theHasWithKeyword )
			  { hasWithKeyword = theHasWithKeyword; }



  public DataCompoundStatement getTaskBody() { return taskBody; }
  public void                  setTaskBody( DataCompoundStatement theTaskBody )
  {
    if ( taskBody != null )
      taskBody . setParent ( null);

    taskBody = theTaskBody;

    if ( taskBody != null )
      taskBody . setParent ( this );
  }



  public DataSpawnTask getExceptionBaseTask() { return exceptionBaseTask; }
  public boolean       setExceptionBaseTask(DataSpawnTask theExceptionBaseTask)
  {
    if (   (   ( theExceptionBaseTask                            != null )
	    && ( theExceptionBaseTask . getTaskName()            != null )
	    && ( theExceptionBaseTask . getTaskName() . length() >  0    )
	    && ( theExceptionBaseTask . getConstraintCount()     == 0    ) )
	|| (     theExceptionBaseTask == null                              ) )
    {
      exceptionBaseTask = theExceptionBaseTask;
      return true;
    }
    else
    {
      System.err.println ( "[DataTaskDefinition:setExceptionBaseTask]  Error: "
			   + "Bad object for theExceptionBaseTask:  " 
			   + theExceptionBaseTask . toString() );
      return false;
    }
  }


  public String  getHandlesException() { return handlesException; }
  public void    setHandlesException( String theHandlesException )
  {
    handlesException = theHandlesException;
  }


  public DataVector getResumeTasksVector() { return resumeTasksVector; }

  public DataTaskDefinition getResumeTask ( int theIndex )
  {
    if ( (theIndex >= 0) && (theIndex < (getResumeTasksVector() . count())) )
      return (DataTaskDefinition)
	     (getResumeTasksVector() . elementAt ( theIndex ));
    else
    {
      System.err.println ( "[DataTaskDefinition:getResumeTask]  Error: "
			   + "Bad resume-task index value (" + theIndex + ")");
      return null;
    }
  }

  public void clearResumeTasksVector()
		{ getResumeTasksVector() . removeAllElements(); }

  public void addResumeTask ( DataTaskDefinition theResumeTask )
		{ getResumeTasksVector() . addElement (theResumeTask); }


  public DataTaskDefinition  getResumeMasterTask() { return resumeMasterTask; }
  public void   setResumeMasterTask ( DataTaskDefinition theResumeMasterTask )
				    { resumeMasterTask = theResumeMasterTask; }



  public String getUniqueIdString()
  {
    if ( getTaskType() == DataTaskDefinition.RESUME_TASK )
    {
      if ( getResumeMasterTask() != null )
	return getResumeMasterTask() . getUniqueIdString();
      else if ( getIsExtern() )
	return uniqueIdString;
      else
	throw new CompilationException (
		     getMessageFilenameLead() + getLineNumberString()
		   + ":  INTERNAL Error:  Resume task named \""
		   + getTaskScopeAndName()
		   + "\" has no corresponding registered counterpart." );
    }
    else
      return uniqueIdString;
  }

	/* Invoked by DataFile to set more reasonable unique-ids */
  public void setUniqueIdString ( String theUniqueIdString )
  {
    uniqueIdString = theUniqueIdString;
  }




  public boolean addPrimaryChild ( DataComponent theChildToAdd,
				   DataComponent theAddChildAfterThisComponent)
  {
    if ( getTaskBody() == null )
      setTaskBody ( new DataCompoundStatement ( null ) );

    return getTaskBody() . addChild ( theChildToAdd,
				      theAddChildAfterThisComponent );
  }

  public boolean removeChild ( DataComponent theChildToRemove )
  {
    if ( getTaskBody() == null )
    {
      System.err.println ( "[DataTaskDefinition:removeChild]  Warning:  "
			   + "No task body to remove child from..." );
      return false;
    }

    return getTaskBody() . removeChild ( theChildToRemove );
  }



  public void validateExternalCode( int                         theReference,
				    DataValidateCodeReturnValue theReturnValue)
    throws CompilationException
  {
    int   i,
          maximumActivateIndex = -1,
          maximumTriggerIndex  = -1,
          maximumPeriodIndex   = -1;

	/* Double check that, if we are an "EXCEPTION HANDLER" task,
	 * we are handling an exception...
	 */
    if (   ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
	&& (   ( getHandlesException()             ==  null )
	    || ( getHandlesException() . length()  <=  0    ) ) )
    {
      theReturnValue
	. addError ( this )
	. write ( "EXCEPTION HANDLER does not handle an exception.\n" );
    }

	/* Deal with inappropriate PERSISTENT qualifiers. */ 
    if ( getIsPersistent() )
    {
      if (DataTaskDefinition.VALID_PERSISTENT_TASKS [ getTaskType() ] == false)
      {
	theReturnValue
	  . addError ( this )
	  . write ( getTaskTypeString() )
	  . write ( " Tasks may NOT be PERSISTENT!\n" );
      }
    }

	/* Deal with inappropriate DISTRIBUTED qualifiers. */ 
    if ( getIsDistributed() )
    {
      if ( DataTaskDefinition.VALID_DISTRIBUTED_TASKS [getTaskType()] == false)
      {
	theReturnValue
	  . addError ( this )
	  . write ( getTaskTypeString() )
	  . write ( " Tasks may NOT be DISTRIBUTED!\n" );
      }
    }


	/* Deal with inappropriate THREADED qualifiers. */ 
    if ( getIsThreaded() )
    {
      if ( DataTaskDefinition.VALID_THREADED_TASKS [ getTaskType() ] == false )
      {
	theReturnValue
	  . addError ( this )
	  . write ( getTaskTypeString() )
	  . write ( " Tasks may NOT be THREADED!\n" );
      }
    }

	/* Deal with inappropriate STATIC qualifiers. */ 
    if ( getIsStatic() )
    {
      if ( DataTaskDefinition.VALID_STATIC_TASKS [ getTaskType() ] == false )
      {
	theReturnValue
	  . addError ( this )
	  . write ( getTaskTypeString() )
	  . write ( " Tasks may NOT be STATIC!\n" );
      }
    }

	/* Deal with inappropriate VIRTUAL qualifiers. */ 
    if ( getIsVirtual() )
    {
      if ( DataTaskDefinition.VALID_VIRTUAL_TASKS [ getTaskType() ] == false )
      {
	theReturnValue
	  . addError ( this )
	  . write ( getTaskTypeString() )
	  . write ( " Tasks may NOT be VIRTUAL!\n" );
      }
    }


	/* Do our Task-level Validation */
    validateTaskForCxxGeneration ( theReference, theReturnValue );



	/* Check for duplicated constraints */
    DataStatement.validateAnyOverridenConstraints ( this, theReturnValue );


	/* At this point, we switch from being external-to-task code
	 * to being internal-to-task code.
	 */
    if ( getTaskBody() != null )
      getTaskBody() . validateInternalCode ( theReference, theReturnValue );
  }



	/* Validates code that is inside of a Task */
  public void validateInternalCode( int                         theReference,
				    DataValidateCodeReturnValue theReturnValue)
    throws CompilationException
  {
    throw new CompilationException ( "Internal Error:  DataTaskDefinition "
				     + "object may not exist inside a Task." );
  }





  public String getWarnString ( int theObjectSubset )
  {
    return super . getWarnString ( theObjectSubset )
      + " or DataTaskDefinition.TASK_WITHOUT_BODY ("
      + DataTaskDefinition.TASK_WITHOUT_BODY + ")"
      + " or DataComponent.HTML_DOCUMENTATION ("
      + DataComponent.HTML_DOCUMENTATION
      + ") or DataTaskDefinition.TASK_AFTER_OPEN_PAREN ("
      + DataTaskDefinition.TASK_AFTER_OPEN_PAREN + ")";
  }

  public boolean isValidObjectSubset ( int theObjectSubset )
  {
    return
         ( theObjectSubset == DataTaskDefinition.TASK_WITHOUT_BODY     )
      || ( theObjectSubset == DataComponent.HTML_DOCUMENTATION         )
      || ( theObjectSubset == DataTaskDefinition.TASK_AFTER_OPEN_PAREN )
      || super . isValidObjectSubset ( theObjectSubset );
  }


  public boolean isValid ( int theObjectSubsetToValidate )
  {
    int i;

    if ( DataComponent . isEmptyString ( getTaskName() ) )
      return false;

    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
      if ( getTaskArgument ( i ) . isValid() == false )
	return false;
    }

    for ( i=0;  i < getPersistentTaskDeclarationCount();  i++ )
    {
      if ( getPersistentTaskDeclaration ( i ) . isValid() == false )
	return false;
    }

    if (   ( theObjectSubsetToValidate != DataTaskDefinition.TASK_WITHOUT_BODY)
	&& ( getTaskBody()             != null  )
	&& ( getTaskBody() . isValid() == false ) )
    {
      return false;
    }

    if (   ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
	&& (   ( getHandlesException()             ==  null )
	    || ( getHandlesException() . length()  <=  0    ) ) )
    {
      return false;
    }

    return super . isValid ( theObjectSubsetToValidate );
  }


  public void generate ( DataDestination  theOutputDestination,
			 int              theObjectSubsetToGenerate )
  {
    generate ( theOutputDestination, theObjectSubsetToGenerate, 
	       TDLC.NO_SUBSET, false );
  }

  public void generate ( DataDestination  theOutputDestination,
			 int              theObjectSubsetToGenerate,
			 int              theDistributedExceptionsMethod,
			 boolean          theShowClassCode )

  {
    int      objectSubsetToGenerate = theObjectSubsetToGenerate;
    int      i;
    String   taskTypeStrings[];
    int      subcomponentNewlineCount;
    String   outputDestinationNewlineText;
    boolean  wasStrippingLeadingWhitespace;

       	/* Print a warning if necessary... */
    warnIfInvalidObjectSubset ( theObjectSubsetToGenerate, "generate" );

    if ( ( getTaskType() == DataTaskDefinition.EXCEPTION_TASK &&
	   getIsDistributed() )
	 ? ( theDistributedExceptionsMethod == 
	     DataComponent.CXX_DISTRIBUTED_EXCEPTIONS_NONE )
	 : ( theDistributedExceptionsMethod == 
	     DataComponent.CXX_DISTRIBUTED_EXCEPTIONS_ONLY ) ) {
      return;
    }

	/* If we are doing HTML generation... */
    if ( theObjectSubsetToGenerate == DataComponent.HTML_DOCUMENTATION )
    {
      generateHTML ( theOutputDestination );
      return;
    }

	/* If we are doing CXX generation... */
    if ( isCxxSubset ( theObjectSubsetToGenerate ) )
    {
      generateCxx ( theOutputDestination, theObjectSubsetToGenerate,
		    theShowClassCode );
      return;
    }

	/* If we are just printing the task-head, */
        /* Let all non-task-body generations act as ENTIRE_OBJECT */
    if (   ( theObjectSubsetToGenerate
				== DataTaskDefinition.TASK_WITHOUT_BODY     )
	|| ( theObjectSubsetToGenerate
				== DataTaskDefinition.TASK_AFTER_OPEN_PAREN ) )
    {
      objectSubsetToGenerate = DataComponent.ENTIRE_OBJECT;
    }

    if ( theObjectSubsetToGenerate == DataTaskDefinition.TASK_AFTER_OPEN_PAREN)
    {
      initializeGenerateSubcomponentIndex ( DataTaskDefinition.OPEN_PAREN );
    }
    else
    {
	/* Initialize us to generate non-significant tokens... */
      initializeGenerateSubcomponentIndex();

	/* Handle leading newline (commenting out) text gracefully */
      outputDestinationNewlineText = theOutputDestination . getNewlineText();
      theOutputDestination . clearNewlineText();

	/* Deal with #line number macros */
      if ( theOutputDestination . getEnableLineMacros() == true )
      {
	    /* Handle stripping leading whitespace gracefully */
	wasStrippingLeadingWhitespace
	  = theOutputDestination . getIsStrippingLeadingWhitespace();

	theOutputDestination . setStripLeadingWhitespace ( false );

	    /* Compute our leading newline count... */
	subcomponentNewlineCount
	  = countSubcomponentNewlines ( DataTaskDefinition.TASK_NAME_INDEX,
					theObjectSubsetToGenerate,
					false /* Don't stop at first *
					       * non-whitespace char */ );

	theOutputDestination . setUsingTdlFileName(  true );
	theOutputDestination . makeNextLineNumber (  getLineNumber()
						   - subcomponentNewlineCount);
	theOutputDestination . write ( "\n" );

	    /* Restore things back, however they were.... */
	theOutputDestination
	  . setStripLeadingWhitespace ( wasStrippingLeadingWhitespace );
      } /* if ( theOutputDestination . getEnableLineMacros() == true ) */


	/* Write any pre-first-token non-significant tokens */
      generateSubcomponents ( DataComponent.FIRST_TOKEN_INDEX,
			      theOutputDestination,
			      objectSubsetToGenerate, false );


	/* TaskLeads == "extern" / "persistent" / "distributed" / etc... */
      if ( hasTaskLeads() )
      {
		/* Write them out, with commenting... */
	for ( i = 0;   i < getTaskLeads() . size();   i ++ )
	{
	  generateSubcomponents ( (String) (getTaskLeads() . elementAt (i)),
				  theOutputDestination,
				  objectSubsetToGenerate, false );

	  if (    DataComponent.isEmptyString ( outputDestinationNewlineText )
	       == false )
	  {
	    theOutputDestination
	      . setNewlineText ( outputDestinationNewlineText );
	    theOutputDestination . write ( outputDestinationNewlineText );
	    outputDestinationNewlineText = null;
	  }

	  theOutputDestination
	    . write ( (String) (getTaskLeads() . elementAt ( i )) );
	}
      }
      else
      {
		/* Clear all these out anyway... */
	for ( i = 0;  i < DataTaskDefinition.TASK_LEADS.length;  i++ )
	{
	  generateSubcomponents ( DataTaskDefinition.TASK_LEADS [ i ],
				  theOutputDestination,
				  objectSubsetToGenerate, false );
	}
      }



	/* Prepare to write the task-type strings (Goal,Command,etc.) */
      taskTypeStrings = getTaskTypeStrings();
      for ( i=0;  i < taskTypeStrings.length;  i++ )
      {
	  /* Write any pre-task-type non-significant tokens */
	generateSubcomponents( (  ( (i+1) < taskTypeStrings.length )
				? ( DataTaskDefinition.TASK_TYPE_INDEX + i )
				: ( DataTaskDefinition.TASK_TYPE_INDEX     ) ),
			       theOutputDestination,
			       objectSubsetToGenerate, false );

	if (    DataComponent.isEmptyString ( outputDestinationNewlineText )
	     == false )
	{
	  theOutputDestination . setNewlineText (outputDestinationNewlineText);
	  theOutputDestination . write ( outputDestinationNewlineText );
	  outputDestinationNewlineText = null;
	}

	  /* Write "task-type" (Goal,Command,etc.) */
	theOutputDestination . write ( taskTypeStrings [ i ] );
      }


	/* Write the task scope.  */
      if ( getWasExplicitlyScoped() )
      {
	getTaskScope() . generate ( theOutputDestination,
				    DataComponent.ENTIRE_OBJECT );
      }


	/* Write any pre-task-name non-significant tokens */
      generateSubcomponents ( DataTaskDefinition.TASK_NAME_INDEX,
			      theOutputDestination,
			      objectSubsetToGenerate, false );

	/* Write "task-name" */
      theOutputDestination . write ( getTaskName() );

	/* Write any start-task-arguments non-significant tokens */
      generateSubcomponents ( DataTaskDefinition.OPEN_PAREN,
			      theOutputDestination,
			      objectSubsetToGenerate, false );

	/* Write " ( " */
      theOutputDestination . write ( DataTaskDefinition.OPEN_PAREN );
    } /* IF ( theObjectSubsetToGenerate == TASK_AFTER_OPEN_PAREN ) ... ELSE */



	/* Write the arguments */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
      getTaskArgument ( i ) . generate ( theOutputDestination,
					 objectSubsetToGenerate );

	/* Write "," if necessary */
      if ( (i+1) < getTaskArgumentCount() )
	theOutputDestination . write ( DataTaskDefinition.COMMA );
    }

	/* Write any end-task-arguments non-significant tokens */
    generateSubcomponents ( DataTaskDefinition.CLOSE_PAREN,
			    theOutputDestination,
			    objectSubsetToGenerate, false );

	/* Write ")" */
    theOutputDestination . write ( DataTaskDefinition.CLOSE_PAREN );
    


	/* If this is an extern task... */
    if ( getIsExtern() )
    {
	/* Write any end-task-arguments non-significant tokens */
      generateSubcomponents ( DataTaskDefinition.SEMICOLON,
			      theOutputDestination,
			      objectSubsetToGenerate, false );

	/* Write ";" */
      theOutputDestination . write ( DataTaskDefinition.SEMICOLON );

	/* Do NOT write the constraints or the body. */
	/* Extern Tasks can't have any constraints or body statements... */
    }

    else
    {
      if (   ( getTaskType()          == DataTaskDefinition.EXCEPTION_TASK )
	  && ( getExceptionBaseTask() != null                              ) )
      {
		/* Write any pre-colon non-significant tokens */
	generateSubcomponents ( DataTaskDefinition.COLON,
				theOutputDestination,
				objectSubsetToGenerate, false );
		/* Write ":" */
	theOutputDestination . write ( DataTaskDefinition.COLON );

		/* Write the exception base task. */
	getExceptionBaseTask() . generate ( theOutputDestination,
					    DataSpawnTask.TASK_ONLY );
      }


      if ( getHasWithKeyword() == true )
      {
	/* Write any end-task-arguments non-significant tokens */
	generateSubcomponents ( DataTaskDefinition.WITH,
				theOutputDestination,
				objectSubsetToGenerate, false );

	theOutputDestination . write ( DataTaskDefinition.WITH );
      }

	  /* Write the constraints */
      for ( i=0;  i < getConstraintsCount();  i++ )
      {
	    /* Write "," if necessary */
	if ( i > 0 )
	  theOutputDestination . write ( DataTaskDefinition.COMMA );

	getConstraint ( i ) . generate ( theOutputDestination,
					 objectSubsetToGenerate );

      } /* for ( i=0;  i < getConstraintsCount();  i++ ) */


      if ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
      {
	if ( getConstraintsCount() > 0 )
	  theOutputDestination . write ( DataTaskDefinition.COMMA );

		/* Write any pre-"handles" non-significant tokens */
	generateSubcomponents ( DataTaskDefinition.HANDLES,
				theOutputDestination,
				objectSubsetToGenerate, false );
		/* Write "handles" */
	theOutputDestination . write ( DataTaskDefinition.HANDLES );

		/* Write any pre-handles-string non-significant tokens */
	generateSubcomponents ( DataTaskDefinition.HANDLES_INDEX,
				theOutputDestination,
				objectSubsetToGenerate, false );
		/* Write handles-string */
	theOutputDestination . write ( getHandlesException() );
      }
      

	    /* Write the Persistent Task Declarations */
      for ( i=0;  i < getPersistentTaskDeclarationCount();  i++ )
      {
	    /* Write "," if necessary */
	if (   ( getConstraintsCount() >  0                               )
	    || ( getTaskType()         == DataTaskDefinition.HANDLER_TASK )
	    || ( i                     >  0                               ) )
	{
		/* Write any pre-handles-comma non-significant tokens */
	  if (   ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
	      && ( i             == 0                               ) )
	  {
	    generateSubcomponents ( DataTaskDefinition.HANDLES_COMMA_INDEX,
				    theOutputDestination,
				    objectSubsetToGenerate, false );
	  }

	  theOutputDestination . write ( DataTaskDefinition.COMMA );
	}

	getPersistentTaskDeclaration( i ) . generate( theOutputDestination,
						      objectSubsetToGenerate );
      }


	/* Write end-of-Exception semicolon? */
      if ( getTaskType() == DataTaskDefinition.EXCEPTION_TASK )
      {
		/* Write any pre-semicolon non-significant tokens */
	generateSubcomponents ( DataTaskDefinition.SEMICOLON,
				theOutputDestination,
				objectSubsetToGenerate, false );

		/* Write ";" */
	theOutputDestination . write ( DataTaskDefinition.SEMICOLON );
      }
      
	/* Write out our body? */
      else if (   ( theObjectSubsetToGenerate
				  != DataTaskDefinition.TASK_WITHOUT_BODY     )
	       && ( theObjectSubsetToGenerate
				  != DataTaskDefinition.TASK_AFTER_OPEN_PAREN )
	       && ( getTaskBody() != null )
	 )
      {
		/* Write any pre-body non-significant tokens */
	generateSubcomponents ( DataTaskDefinition.TASK_BODY_INDEX,
				theOutputDestination,
				objectSubsetToGenerate, false );

	getTaskBody() . generate ( theOutputDestination,
				   theObjectSubsetToGenerate );
      }

    } /* IF ( getIsExtern() ) ....  ELSE .... */

	/* Write any remaining non-significant tokens */
    generateAllRemainingSubcomponents ( theOutputDestination,
					theObjectSubsetToGenerate, false );
  }


  public void generateHTML ( DataDestination  theOutputDestination )
  {
    int             index = 0;
    int             startPosition, endPosition;
    int             firstNewline,  secondNewline, matchCharCount;
    int             i, j, lastNewline;
    String          string;
    StringBuffer    stringBuffer;
    DataComponent   dataComponent = getParent();
    
	/* Find our containing DataFile object */
    while (    (  dataComponent                      != null  )
	    && ( (dataComponent instanceof DataFile) == false ) )
    {
      dataComponent = dataComponent . getParent();
    }


	/* Write heading... */
    theOutputDestination . write ( "\n<A NAME=\"" );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( "\">\n<PRE><H2><TT>" );
    theOutputDestination . write ( getTaskTypeString() );
    theOutputDestination . write ( " <A HREF=\"file:" );
    if ( dataComponent != null )
    {
      if ( DataComponent.isEmptyString ( ((DataFile) (dataComponent))
					   . getFilename()           ) )
      {
	theOutputDestination . write ( "/**UNSPECIFIED**" );
      }
      else
      {
	if ( ((DataFile) (dataComponent)) . getFilename()
	                                  . startsWith ( "/" ) == false )
	{
	  theOutputDestination . write ( "/" );
	}
	theOutputDestination . write ( ((DataFile) (dataComponent))
				         . getFilename() );
      }
    }
    else /* ( dataComponent == null ) */
    {
      theOutputDestination . write ( "/**UNKNOWN**" );
    }
    theOutputDestination . write ( "\">" );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( "</A> (" );


	/* Write this Task out... */
    generate ( theOutputDestination, DataTaskDefinition.TASK_AFTER_OPEN_PAREN);

	/* Conclude heading... */
    theOutputDestination . write ( "</TT></H2></PRE></A>\n<DD><P>" );



	/* Find any task documentation comments */
    if ( hasTaskLeads() )
    {
      index = getIndex ( (String) (getTaskLeads() . elementAt ( 0 ) ) );
    }
    else
    {
      index = getIndex ( DataTaskDefinition.TASK_TYPE_INDEX );
    }

    for ( index --;   index >= 0;   index -- )
    {
      if ( isSubcomponentAString ( index ) )
      {
	string = getStringSubcomponent ( index );
	startPosition  = string.length();
	endPosition    = -1;

	while (  (startPosition != -1)  &&  (endPosition == -1)  )
	{
	  startPosition = string . lastIndexOf ( "/**", startPosition - 1 );
	  if ( startPosition != -1 )
	    endPosition   = string . indexOf ( "*/", startPosition );
	}

	    /* If we found our document-string, try to clean it up a bit
	     * Folks like to add leading nonsense to their comments
	     * to make them look nice...
	     *
	     * (Nonsense that is pure hell to try and remove, but has to be
	     *  removed...  So this code below looks for matching regions
	     *  following the first two newlines, and dikes them out.
	     *  Basically, and quick & dirty approach to a messy problem...)
	     */
	if (  (startPosition != -1)  &&  (endPosition != -1)  )
	{
	  string = string.substring ( startPosition + 3, endPosition + 1 );

	  /*  (Debugging code)
	   * System.err.println("string=" + string );
	   */

		/* Remove tabs from the string... */
	  if ( string . indexOf ( '\t' ) != -1 )
	  {
	    stringBuffer = new StringBuffer();

	    for ( lastNewline=0, i=0;   i < string.length();   i++ )
	    {
	      if ( string.charAt ( i ) == '\n' )
		lastNewline = i;

	      if ( string.charAt ( i ) != '\t' )
	      {
		stringBuffer . append ( string.charAt ( i ) );
	      }

	      else /*  We have a tab */
	      {
		for ( j=0;
		      j < ( DataComponent.getTabSize()
			    - ( (i - lastNewline - 1)
				% DataComponent.getTabSize() ) );
		      j++ )
		{
		  stringBuffer . append ( ' ' );
		}
		   /* Do a magical correction to factor in the new spaces */
		   /* we just added for subsequent tabs...                */
		if ( j > 0 )
		  lastNewline -= (j - 1);
	      }
	    } /* for ( lastNewline=0, i=0;   i < string.length();   i++ ) */

	    string = stringBuffer . toString();
	  }


	  firstNewline  = string . indexOf ( "\n" );
	  secondNewline = string . indexOf ( "\n", firstNewline + 1 );

	    /* If there are not at least 2 newlines, do the simple route */
	  if (  ( firstNewline == -1 )  ||  ( secondNewline == -1 )  )
	  {
	    theOutputDestination
	      . write ( string . substring ( 0, string . length() - 1 ) );
	  }
	  else  /* Try to do a patern-cleanup here... */
	  {
		/* Find the match-patern */
	    for ( matchCharCount = 1 ;

		     ( (matchCharCount + firstNewline ) <= secondNewline      )
		  && ( (matchCharCount + secondNewline) <= string.length()    )
		  && (   ( Character.isWhitespace (
			     string . charAt (   matchCharCount
					       + firstNewline   ) ) == true )
		      || (   string . charAt (   matchCharCount
					       + firstNewline   )   == '*'  ) )
		  && string . regionMatches ( false,
					      firstNewline,
					      string,
					      secondNewline,
					      matchCharCount );

		  matchCharCount ++ )
	    {
	      ; /* NULL / EMPTY FOR BODY STATEMENT */
	    }

		/* Forget about the last match-char */
	    matchCharCount --;
	    /*   (Debugging Code)
	     * System.err.println("Matching..: " + matchCharCount );
	     * System.err.println( "..Matching: \""
	     *  + string . substring ( firstNewline,
	     *   		      firstNewline + matchCharCount ) + "\"" );
	     */

		/* Trivial case... */
	    if ( matchCharCount <= 1 )
	    {
	      theOutputDestination
		. write ( string . substring ( 0, string . length() - 1 ) );
	    }
	    else
	    {
	      startPosition = 0;
	      while ( startPosition < string.length() - 1 )
	      {
		endPosition = string . indexOf ( "\n", startPosition );
		if ( endPosition == -1 )
		  endPosition = string.length() - 1;

		theOutputDestination
		  . write ( string.substring ( startPosition, endPosition  ) );
		theOutputDestination . write ( "\n" );

		if (   ( (matchCharCount + endPosition) < string.length() )
		    && ( string . regionMatches ( false,
						  firstNewline,
						  string,
						  endPosition,
						  matchCharCount ) ) )
		{
		  startPosition = endPosition + matchCharCount;
		}
		else
		{
		  startPosition = endPosition + 1;
		}
	      } /* while ( startPosition < string.length() ) */
	    } /* if ( matchCharCount <= 1 ) ... else ... */
	  } /* if ( no first/second newline ) ... else ... */


	  break;  /* Exit for loop */

	} /* if ( startPosition AND endPosition != -1 -- found Doc string ) */
      } /* if ( isSubcomponentAString ( index ) ) */
    } /* for ( index --;   index >= 0;   index -- ) */

    theOutputDestination . write ( "</P></DD>\n<BR><BR>\n\n" );
  } /* public void generateHTML ( DataDestination  theOutputDestination ) */


	/* Convenience method.  This grouping of subsets is not what one
	 * might ordinarily think, but it is most useful for generateCxx()
	 */
  private boolean shouldDoCxxHeaderClassFor ( int  theSubset )
		     { return shouldDoCxxHeaderClassFor ( theSubset, false ); }

  private boolean shouldDoCxxHeaderClassFor ( int     theSubset,
					      boolean theAllowResumeTasks )
  {
    return ( getIsExtern() == false )
       &&  (   ( getTaskType()       != DataTaskDefinition.RESUME_TASK   )
	    || ( theAllowResumeTasks == true                             ) )
       &&  (   ( theSubset == DataComponent.CXX_HEADER                   )
	    || ( theSubset == DataComponent.INLINED_CXX_CODE             )
	    || ( theSubset == DataComponent.CXX_HEADER_INLINED_FUNCTIONS )

		/* Note:  EXCEPTION Tasks must always have the class defined
		 * in the header file...  Otherwise, inheritance brakes down,
		 * and EXCEPTION HANDLERs can't be defined in other files...
		 */
	    || (   (   ( getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
		    && ( theSubset     == DataComponent.CXX_CODE_AND_HEADER ) )

		|| (   ( getTaskType() == DataTaskDefinition.EXCEPTION_TASK )
		    && ( theSubset     == DataComponent.CXX_BARE_HEADER     ) )
	       )
	    );
  }

	/* Convenience method.  This grouping of subsets is not what one
	 * might ordinarily think, but it is most useful for generateCxx()
	 */
  private boolean shouldDoCxxCodeFor ( int  theSubset )
  {
    return ( getIsExtern() == false )
       &&  (   ( theSubset == DataComponent.CXX_CODE              )
	    || ( theSubset == DataComponent.CXX_CODE_NO_FUNCTIONS ) );
  }

	/* Only enable default arguments in the header... */
  private boolean shouldDoDefaultArgumentsFor ( int theSubset )
  {
    return
      (   ( theSubset     == DataComponent.CXX_HEADER                         )
       || ( theSubset     == DataComponent.CXX_BARE_HEADER                    )
       || ( getIsExtern() == true                                             )
       || ( theSubset     == DataComponent.INLINED_CXX_CODE                   )
       || ( theSubset     == DataComponent.CXX_HEADER_INLINED_FUNCTIONS       )
       || ( theSubset     == DataComponent.CXX_HEADER_DISTRIBUTED_ONLY        )
       || ( theSubset     == DataComponent.CXX_HEADER_INLINED_DISTRIBUTED_ONLY)
      );
  }


	/* Convenience method.  Some Tasks do not have an Execute method. */
  private boolean hasExecuteMethod()
  {
    return getTaskType() != DataTaskDefinition.EXCEPTION_TASK;
  }



  private boolean needsUniqueIdString()
  {
    switch ( getTaskType() )
    {
      case DataTaskDefinition.GOAL_TASK:
      case DataTaskDefinition.COMMAND_TASK:
      case DataTaskDefinition.MONITOR_TASK:
      case DataTaskDefinition.HANDLER_TASK:
	return true;

      case DataTaskDefinition.RESUME_TASK:
	if ( getResumeMasterTask() != null )
	  return getResumeMasterTask() . needsUniqueIdString();
	else if ( getIsExtern() )
	  return false;
	else
	  throw new CompilationException (
		     getMessageFilenameLead() + getLineNumberString()
		   + ":  INTERNAL ERROR:  Resume task named \""
		   + getTaskScopeAndName()
		   + "\" has no corresponding registered counterpart." );

      default:
	return false;
    }
  }

  private void writeUniqueIdStringIfNeeded (
					DataDestination  theOutputDestination )
  {
    if ( needsUniqueIdString() )
      theOutputDestination . write ( getUniqueIdString() );
  }

  private void writeOverloadingBackwardCompatibilityIfdefIfNeeded (
					DataDestination  theOutputDestination,
					String           theSimpleName        )
  {
    if ( needsUniqueIdString() )
    {
      theOutputDestination . write ( "#ifndef " );
      theOutputDestination . write ( theSimpleName );
      theOutputDestination . write ( "\n#define " );
      theOutputDestination . write ( theSimpleName );
      theOutputDestination . write ( " " );
      theOutputDestination . write ( theSimpleName );
      theOutputDestination . write ( getUniqueIdString() );
      theOutputDestination . write ( "\n#endif /*" );
      theOutputDestination . write ( theSimpleName );
      theOutputDestination . write ( "*/\n" );
    }
  }



  public void generateCxx ( DataDestination  theOutputDestination,
			    int              theSubsetToProduce,
			    boolean          theShowClassCode )
    throws CompilationException
  {
    generateCxx ( theOutputDestination,

		  theSubsetToProduce,

			/* theShouldPrintTaskAsComment */
		  (       shouldDoCxxHeaderClassFor  ( theSubsetToProduce,
						       true               )
		   ||     shouldDoCxxCodeFor         ( theSubsetToProduce )
		   || (   isCxxDistributedOnlySubset ( theSubsetToProduce )
		       && ( getIsDistributed() == true )                   ) ),

			/* theNonUniqueNames */
		  null,

		  theShowClassCode );
  }


	/* Note:  For Resume Tasks Only:
	 *          Headers do nothing via shouldDoCxxHeaderClassFor().
	 *          (resume() header is generated through attached task.)
	 *          Trailers only generate _TDL_...::resume().
	 */
  protected void generateCxx( DataDestination  theOutputDestination,
			      int              theSubsetToProduce,
			      boolean          theShouldPrintTaskAsComment,
			      DataVector       theNonUniqueNames,
			      boolean          theShowClassCode )
    throws CompilationException
  {
    int                 i, tmpIndent,
                        persistentInitializerIndent;
    String              cxxNameLead              = null;
    boolean             needsNewline;
    DataTaskDefinition  masterDataTaskDefinition = this;


	/* Can't generate inside-class headers outside of a class... */
    if (   ( theShowClassCode == false                )
	&& ( getTaskScope() . hasScope()              )
	&& ( getWasImplicitlyScoped()                 )
	&& ( isCxxHeaderSubset ( theSubsetToProduce ) ) )
      return;

	/* Can't generate explicity scoped headers... */
    if (   ( getTaskScope() . hasScope()              )
	&& ( getWasExplicitlyScoped()                 )
	&& ( isCxxHeaderSubset ( theSubsetToProduce ) ) )
      return;


	/* Obtain the "master" task definition in case this is a Resume Task */
    if ( getTaskType() == DataTaskDefinition.RESUME_TASK )
    {
      masterDataTaskDefinition = getResumeMasterTask();

	/* Check for idiocy. */
      if ( masterDataTaskDefinition == null )
      {
	if ( getIsExtern() == true )
	{
	  generateCxxResumeFunction ( theOutputDestination, theSubsetToProduce,
			  "This_Is_Extern_So_This_String_Should_Never_Be_Used"
			  +"_And_If_You_See_It_You_Have_Found_A_Bug." );
	  return;
	}
	else
	{
	  throw new CompilationException (
		     getMessageFilenameLead() + getLineNumberString()
		   + ":  Internal Error:  Resume task named \""
		   + getTaskScopeAndName()
		   + "\" has no corresponding registered counterpart." );
	}
      }
    }


	/* Figure out our cxxNameLead String... */
    switch ( masterDataTaskDefinition . getTaskType() )
    {
      case DataTaskDefinition.EXCEPTION_TASK:
	cxxNameLead = DataComponent.CXX_EXCEPTION_NAME_LEAD;
	break;

      case DataTaskDefinition.HANDLER_TASK:
	cxxNameLead = DataComponent.CXX_HANDLER_NAME_LEAD;
	break;

      default:
	cxxNameLead = DataComponent.CXX_NAME_LEAD;
	break;
    }


	/* Idiocy check... */
    if (   ( theSubsetToProduce != DataComponent.CXX_CODE                     )
	&& ( theSubsetToProduce != DataComponent.CXX_HEADER                   )
	&& ( theSubsetToProduce != DataComponent.INLINED_CXX_CODE             )
	&& ( theSubsetToProduce != DataComponent.CXX_CODE_NO_FUNCTIONS        )
	&& ( theSubsetToProduce != DataComponent.CXX_HEADER_INLINED_FUNCTIONS )
	&& ( theSubsetToProduce != DataComponent.CXX_CODE_AND_HEADER          )
	&& ( theSubsetToProduce != DataComponent.CXX_BARE_HEADER              )
	&& ( isCxxDistributedOnlySubset ( theSubsetToProduce ) == false       )
        )
    {
      System.err.println ( "[DataTaskDefinition:generateCxx]  Warning:  "
	        + "Invalid theSubsetToProduce (" + theSubsetToProduce + ")." );
      return;
    }



	/* Start out on a blank line... */
    theOutputDestination . write ( "\n" );

	/* Write the Task as a leading comment... */
    if ( theShouldPrintTaskAsComment )
    {
      theOutputDestination . setNewlineText ( "  // " );
      generate ( theOutputDestination, DataTaskDefinition.TASK_WITHOUT_BODY );
      theOutputDestination . clearNewlineText();
      theOutputDestination . setUsingTdlFileName ( false );
      theOutputDestination . write ( "\n" );
    }

    if (   ( theSubsetToProduce == DataComponent.CXX_CODE                    )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER                  )
	|| ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE            )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER_INLINED_FUNCTIONS)
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER             )
	|| ( getIsExtern()      == true                                      ))
    {
      if ( getTaskType() == DataTaskDefinition.EXCEPTION_TASK )
      {
	generateCxxCreateException( theOutputDestination, theSubsetToProduce );
      }
    }

	/* Deal with Distributed-Only Generation */
	/* (DON'T need to validate for this...)  */
    if ( isCxxDistributedOnlySubset ( theSubsetToProduce ) )
    {
      if ( getIsDistributed() == true && 
	   getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
      {
	generateCxxAllocate     ( theOutputDestination, theSubsetToProduce );
	generateCxxCreateAction ( theOutputDestination, theSubsetToProduce );
      }
	/* Skip this Task if it's NOT distributed. */
      return;
    }


	/* If we need to do validation -- do it.
	 * (If there's a major problem, lets find it and bomb now!)
	 */
    if (    ( theNonUniqueNames == null  )
	 && ( getIsExtern()     == false ) )
    {
	/*
	 * Caveat:  validateTaskForCxxGeneration has the side effect of
	 * generating certain key data caches.  Specifically, the
	 * statementsVector and nonUniqueNamesVector.  These are used
	 * later on...
	 *
	 * Thus, even if validateTaskForCxxGeneration() was run during
	 * validateExternalCode, we can't rely on any previously cached
	 * values.  We have to run it again.
	 */
      theNonUniqueNames = validateTaskForCxxGeneration();
    }


    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
    {
	/* Write "class ___TDL_TaskName : public BaseClass
	 *        {
	 *        public:
	 */
      theOutputDestination . write ( "class " );
      theOutputDestination . write ( cxxNameLead );
      theOutputDestination . write ( getTaskName() );
      writeUniqueIdStringIfNeeded  ( theOutputDestination );
      theOutputDestination . write ( "  : public " );
      switch ( getTaskType() )
      {
		/* Exceptions have a single inheritance hierarchy... */
	case DataTaskDefinition.EXCEPTION_TASK:
	  if ( getExceptionBaseTask() != null )
	  {
	    theOutputDestination
	      . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
	    theOutputDestination
	      . write ( getExceptionBaseTask() . getTaskName() );
	  }
	  else
	  {
	    theOutputDestination
	      . write ( DataComponent.CXX_EXCEPTION_BASE_CLASS );
	  }
	  break;

	case DataTaskDefinition.HANDLER_TASK:
	  theOutputDestination . write ( DataComponent.CXX_HANDLER_BASE_CLASS);
	  break;

	default:
	  theOutputDestination . write ( DataComponent.CXX_BASE_CLASS );
	  break;
      }
      theOutputDestination . write ( "\n{\n" );
      if (getTaskType() != DataTaskDefinition.EXCEPTION_TASK) {
	  theOutputDestination . write ( "public:\n" );

	  /* Write RTTI methods */
	  theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
	  theOutputDestination . write ( DataComponent.CXX_RTTI_METHODS_PART_ONE );
	  theOutputDestination . write ( getTaskTypeString() );
	  theOutputDestination . write ( "-" );
	  theOutputDestination . write ( getTaskScopeAndName() );
	  writeUniqueIdStringIfNeeded  ( theOutputDestination );
	  theOutputDestination . write ( DataComponent.CXX_RTTI_METHODS_PART_TWO );
	  theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
	  theOutputDestination . write ( "\n\n" );
      }
    } /* if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) ) */


	/* Deal with Distributed Tasks registrations...               *
         * Skipping over header files where the class is not defined! */
    if ( theSubsetToProduce != CXX_BARE_HEADER )
    {
      generateCxxDistributedRegistryEntry (
			    theOutputDestination,
			    theSubsetToProduce,
			    cxxNameLead,
			    getTaskType() == DataTaskDefinition.EXCEPTION_TASK,
			    shouldDoCxxHeaderClassFor ( theSubsetToProduce ) );
    }


	/* Continue writing the class... */
    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
    {
      theOutputDestination . write ( "public:" );


	/* And lets indent the class */
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	/* Deal with #line macros. */
      if (   ( getTaskArgumentCount()              > 0 )
	  || ( getPersistentTaskDeclarationCount() > 0 ) )
      {
	theOutputDestination . setUsingTdlFileName ( true );
      }

	/* Write Task Arguments as Class-Instance Variables */
      for ( i=0;  i < getTaskArgumentCount();  i++ )
      {
	theOutputDestination . makeNextLineNumber (
				     getTaskArgument ( i ) . getLineNumber() );
	theOutputDestination . write ( "\n" );

	theOutputDestination . setStripLeadingWhitespace();
	getTaskArgument ( i ) . generate ( theOutputDestination,
					   DataTaskArgument.TYPE_AND_NAME );
	theOutputDestination . write ( ";" );
      }

      boolean hasPersistentInitializers = false;

	/* Write Persistent Declarations as Class-Instance Variables  */
      for ( i=0;  i < getPersistentTaskDeclarationCount();  i++ )
      {
	theOutputDestination . makeNextLineNumber (
			getPersistentTaskDeclaration ( i ) . getLineNumber() );
	theOutputDestination . write ( "\n" );

	theOutputDestination . setStripLeadingWhitespace();
	getPersistentTaskDeclaration ( i )
	  . generate ( theOutputDestination, DataTaskArgument.TYPE_AND_NAME );
	theOutputDestination . write ( ";" );
	hasPersistentInitializers |=
	    getPersistentTaskDeclaration ( i ) . hasEquals();
      }

	/* Deal with #line macros. */
      if (   ( getTaskArgumentCount()              > 0 )
	  || ( getPersistentTaskDeclarationCount() > 0 ) )
      {
	theOutputDestination . setUsingTdlFileName ( false );
      }

      theOutputDestination . write ( "\n" );



	/* Write the class constructor */
      theOutputDestination . write ( "\n" );
      theOutputDestination . write ( cxxNameLead );
      theOutputDestination . write ( getTaskName() );
      writeUniqueIdStringIfNeeded  ( theOutputDestination );
      theOutputDestination . write ( " ( " );


	/* Need to do more indenting here... */
      tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	/* Deal with #line macros. */
      if ( getTaskArgumentCount() > 0 )
	theOutputDestination . setUsingTdlFileName ( true );

	/* Write Task Arguments as constructor arguments */
      for ( i=0;  i < getTaskArgumentCount();  i++ )
      {
	theOutputDestination . makeNextLineNumber (
				     getTaskArgument ( i ) . getLineNumber() );
	if ( i > 0 )
	  theOutputDestination . write ( ",\n" );
	else if ( theOutputDestination . getEnableLineMacros() )
	  theOutputDestination . write ( "\n" );

	theOutputDestination . setStripLeadingWhitespace();
	getTaskArgument ( i )
	  . generate ( theOutputDestination,
		       DataTaskArgument.TYPE_NAME_AND_EQUALS );
      }

	/* Deal with #line macros. */
      if ( getTaskArgumentCount() > 0 )
	theOutputDestination . setUsingTdlFileName ( false );

        /* Deal with #line macro newline down below... */


        /* For Handlers:  Add Maximum-Activates variable... */
      if ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
      {
	if ( getTaskArgumentCount() > 0 )
	  theOutputDestination . write ( ",\n" );
	theOutputDestination . write (
		DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_ARGUMENT_TYPE );
	theOutputDestination . write ( " " );
	theOutputDestination
	  . write ( DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_ARGUMENT );
	    /*Note: Need this in case previous arguments had a default value.*/
	theOutputDestination . write ( " = " );
	theOutputDestination . write (
	  DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_DEFAULT_VALUE );
      }


	/* Terminate the construtor arguments and write the initializer */
      theOutputDestination . write ( " )" );

	/* stop indenting constructor arguments */
      theOutputDestination . removeIndent ( tmpIndent );

	/* Begin indenting constructor initializer */
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

      if (   ( getTaskArgumentCount() > 0 )

	  || ( hasPersistentInitializers )

	  || (   ( getTaskType()          == DataTaskDefinition.EXCEPTION_TASK)
	      && ( getExceptionBaseTask() != null                             )
	      )

	  || ( getTaskType()              == DataTaskDefinition.HANDLER_TASK  )
	  )
      {
	theOutputDestination . write ( "\n: " );

	  /* Begin indenting construtor initializer components */
	tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	  /* Start out not needed a newline */
	needsNewline = false;

	  /* Add base-class initializer, if necessary... */
	if (   ( getTaskType()          == DataTaskDefinition.EXCEPTION_TASK )
	    && ( getExceptionBaseTask() != null                              ))
	{
	  getExceptionBaseTask()
	    . generateTask ( theOutputDestination,
			     DataComponent.ENTIRE_OBJECT,
			     false,  /* No leading space    */
			     false,  /* No leading comments */
			     true,   /* Indent nicely,      */
			     false,  /*   on current line.  */
			     true,   /* Is Cxx generation.  */
			     DataComponent.CXX_EXCEPTION_NAME_LEAD );
	  needsNewline = true;
	}

	  /* For Handlers:  Initialize Maximum-Activates optional-variable...*/
	if ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
	{
	  if ( needsNewline == true )
	    theOutputDestination . write ( ",\n" );
	  else
	    needsNewline = true;

	  theOutputDestination . write ( DataComponent.CXX_HANDLER_BASE_CLASS);
	  theOutputDestination . write ( " ( " );
	  theOutputDestination
	    . write ( DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_ARGUMENT);
	  theOutputDestination . write ( " )" );
	}


	  /* Add arguments. */
	for ( i=0;  i < getTaskArgumentCount();  i++ )
	{
	  if ( needsNewline == true )
	    theOutputDestination . write ( ",\n" );
	  else
	    needsNewline = true;

	  theOutputDestination . write ( getTaskArgument ( i )
				           . getArgumentName() );
	  theOutputDestination . write ( " ( " );
	  theOutputDestination . write ( getTaskArgument ( i )
				           . getArgumentName() );
	  theOutputDestination . write ( " )" );
	}

	  /* Add Persistent declarations */
	for ( i=0;  i < getPersistentTaskDeclarationCount();  i++ )
	{
	  if ( getPersistentTaskDeclaration ( i ) . hasEquals() )
	  {
	    if ( needsNewline == true )
	      theOutputDestination . write ( ",\n" );
	    else
	      needsNewline = true;

	    theOutputDestination . write ( getPersistentTaskDeclaration ( i )
				             . getArgumentName() );
	    theOutputDestination . write ( " ( " );

		/* Indent persistent declaration to this point. */
	    persistentInitializerIndent
	      = theOutputDestination . indentToCurrentColumn ( );

		/* Deal with #line macros */
	    if ( theOutputDestination . getEnableLineMacros() )
	    {
	      theOutputDestination . setUsingTdlFileName ( true );
	      theOutputDestination . makeNextLineNumber (
			getPersistentTaskDeclaration ( i ) . getLineNumber()
		      + getPersistentTaskDeclaration ( i )
			  . countSubcomponentNewlinesAfterEquals() );
	      theOutputDestination . write ( "\n" ); /* Flush #line macro */
	    }

		/* Generate persistent declaration */
	    theOutputDestination . setStripLeadingWhitespace();
	    theOutputDestination . setPersistentlyStripLeadingSpaces( true );
	    getPersistentTaskDeclaration ( i )
	      . generate ( theOutputDestination,
			   DataTaskArgument.AFTER_EQUALS );
	    theOutputDestination . setPersistentlyStripLeadingSpaces( false );

		/* Deal with #line macros */
	    if ( theOutputDestination . getEnableLineMacros() )
	    {
	      theOutputDestination . setUsingTdlFileName ( false );

		/* Put us one space further to the right for the ")" */
	      theOutputDestination . removeIndent ( 1 );
	      persistentInitializerIndent --;

	      theOutputDestination . write ( "\n" ); /* Flush #line macro */
	    }
	    else
	    {
	      theOutputDestination . write ( " " );
	    }

	    theOutputDestination . write ( ")" );

		/* Stop Indenting persistent declaration. */
	    theOutputDestination . removeIndent( persistentInitializerIndent );
	  }
	}

	  /* stop indenting constructor arguments */
	theOutputDestination . removeIndent ( tmpIndent );
      }

	/* stop indenting constructor initializer */
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	/* Write the constructor body... */
      theOutputDestination . write ( "\n{}\n\n\n" );

    } /* if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) ) */



		/*********************************/
		/* Do the virtual execute method */
		/*********************************/
	/* Note:  Generates resume code for Resume Tasks.
	 *        Does nothing (NO-OP) for resume headers.
	 *        Generates execute header/code for other tasks.
	 */
    generateCxxExecuteOrResume ( theOutputDestination, theSubsetToProduce,
				 theNonUniqueNames,    cxxNameLead,
				 false, masterDataTaskDefinition );

	/* Generates the RESUME task headers, if we have them. */
    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
    {
      for ( i = 0;   i < getResumeTasksVector() . count();   i ++ )
      {
	getResumeTask ( i )
	  . generateCxxExecuteOrResume (
	      theOutputDestination, theSubsetToProduce,
	      getResumeTask ( i ) . validateTaskForCxxGeneration(),
	      cxxNameLead, true, masterDataTaskDefinition );
      }
    }



	/* Generate any extra methods */
    if (   shouldDoCxxHeaderClassFor ( theSubsetToProduce )
	|| shouldDoCxxCodeFor        ( theSubsetToProduce ) )
    {
	/* Generate the exception methods */
      if ( getTaskType() == DataTaskDefinition.EXCEPTION_TASK )
      {
	generateCxxGetExceptionData    ( theOutputDestination,
					 theSubsetToProduce   );
	generateCxxStaticExceptionName    ( theOutputDestination,
					    theSubsetToProduce   );
	generateCxxExceptionMatchesString ( theOutputDestination,
					    theSubsetToProduce   );
	generateCxxExceptionCloneString ( theOutputDestination,
					  theSubsetToProduce   );
      }

	/* Generate the handler methods */
      else if ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
      {
	generateCxxHandledExceptionName   ( theOutputDestination,
					    theSubsetToProduce   );
      }
    }


    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
    {
	/* Stop Indenting the class */
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	/* Terminate the class */
      theOutputDestination . write ( "\n}; /* " );
      theOutputDestination . write ( "class " );
      theOutputDestination . write ( cxxNameLead );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( " */\n\n" );
      writeOverloadingBackwardCompatibilityIfdefIfNeeded (
	theOutputDestination, cxxNameLead + getTaskName() );
      theOutputDestination . write ( "\n\n" );

    } /* if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) ) */




	/* Write the #define with-wait macro & the interface-functions */

      /* Note: CXX_CODE_NO_FUNCTIONS has no functions since they are
       *       inlined in it's counterpart CXX_HEADER_INLINED_FUNCTIONS...
       * Note: CXX_CODE_AND_HEADER is (generally) taken care of
       *       on the second pass via CXX_CODE.
       */
    if (   ( theSubsetToProduce == DataComponent.CXX_CODE                    )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER                  )
	|| ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE            )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER_INLINED_FUNCTIONS)
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER             )
	|| ( getIsExtern()      == true                                      ))
    {
      if ( getTaskType() == DataTaskDefinition.EXCEPTION_TASK )
      {
	  /* Moved this to before the class definition */
      }
      else if ( getTaskType() == DataTaskDefinition.HANDLER_TASK )
      {
	generateCxxCreateExceptionHandler ( theOutputDestination,
					    theSubsetToProduce   );
      }
      else if ( getTaskType() == DataTaskDefinition.RESUME_TASK )
      {
	generateCxxResumeFunction ( theOutputDestination, theSubsetToProduce,
				    cxxNameLead );
      }
      else
      {
	generateCxxAllocate     ( theOutputDestination, theSubsetToProduce );
	generateCxxCreateAction ( theOutputDestination, theSubsetToProduce );
	generateCxxCreateDistributedAction (
				  theOutputDestination, theSubsetToProduce );
	generateCxxSpawnAndWait ( theOutputDestination, theSubsetToProduce );
	generateCxxFunctionalInvocation (
				  theOutputDestination, theSubsetToProduce );
      }
    }



	/* SPECIAL CASE:  If we have both the code & the header here... */
    if (   ( theSubsetToProduce == DataComponent.CXX_CODE_AND_HEADER )
	&& ( getIsExtern()      == false ) )
    {
	/* Recurse & do it all over again... */
      generateCxx ( theOutputDestination,
		    DataComponent.CXX_CODE,
		    false,
		    theNonUniqueNames,
		    theShowClassCode );
    }

  } /* void generateCxx ( ... ) */


  protected void generateCxxDistributedExceptionCreator
				 ( DataDestination  theOutputDestination )
  {
    int i, tmpIndent;

      /* Write the header */
    theOutputDestination . write ( "static " );
    theOutputDestination
	. write ( DataComponent.CXX_CREATE_EXCEPTION_RETURN_VALUE);
    theOutputDestination . write ( "\n" );
    theOutputDestination . write ( DataComponent.CXX_CREATOR_FUNCTION_LEAD);
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );
    theOutputDestination . write ( DataComponent.CXX_CREATOR_ARG_EXCEP_TYPE );
    theOutputDestination . write ( " " );
    theOutputDestination . write ( DataComponent.CXX_CREATOR_ARG_EXCEP_NAME );
    theOutputDestination . write ( ",\n" );
    theOutputDestination . write ( DataComponent.CXX_CREATOR_ARG_DATA_TYPE );
    theOutputDestination . write ( " " );
    theOutputDestination . write ( DataComponent.CXX_CREATOR_ARG_DATA_NAME );
    theOutputDestination . removeIndent ( tmpIndent );
    theOutputDestination . write ( " )\n{\n" );
    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
    if ( getTaskArgumentCount() > 0 ) {
        /* Write the struct version of the arguments */
      theOutputDestination
	  . write ( DataComponent.CXX_CREATOR_EXCEP_DATA_TYPE );
        /* Indent the arguments... */
      theOutputDestination . write ( " {" );
      tmpIndent = theOutputDestination . indentToCurrentColumn ( );
      theOutputDestination . write ( " " );
        /* Write the components of the struct */
      for ( i=0;  i < getTaskArgumentCount();  i++ ) {
	getTaskArgument ( i )
	   . generate ( theOutputDestination, DataTaskArgument.TYPE_AND_NAME );
	theOutputDestination . write ( ";\n" );
      }
      theOutputDestination . write ( "} *" );
      theOutputDestination
	  . write ( DataComponent.CXX_CREATOR_EXCEP_DATA_NAME );
      theOutputDestination . write ( ";\n" );
      theOutputDestination . removeIndent ( tmpIndent );

        /* Write the cast */
      theOutputDestination
	  . write ( DataComponent.CXX_CREATOR_EXCEP_DATA_NAME );
      theOutputDestination . write ( " = ( " );
      theOutputDestination
	  . write ( DataComponent.CXX_CREATOR_EXCEP_DATA_TYPE );
      theOutputDestination . write ( " * ) " );
      theOutputDestination . write ( DataComponent.CXX_CREATOR_ARG_DATA_NAME );
      theOutputDestination . write ( ";\n" );
    }

      /* Write the actual creator */
    theOutputDestination . write ( "return " );
    theOutputDestination
	. write ( DataComponent.CXX_CREATE_EXCEPTION_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );
    for ( i=0;  i < getTaskArgumentCount();  i++ ) {
      if ( i > 0 )
	theOutputDestination . write ( ",\n" );
      theOutputDestination
	  . write ( DataComponent.CXX_CREATOR_EXCEP_DATA_NAME );
      theOutputDestination . write ( "->" );
      theOutputDestination . setStripLeadingWhitespace();
      theOutputDestination
	  . write ( getTaskArgument ( i ) . getArgumentName() );
    }
    theOutputDestination . write ( " );\n" );
    theOutputDestination . removeIndent ( tmpIndent );
    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
    theOutputDestination . write ( "}\n\n" );
  }

  protected void generateCxxDistributedRegistryEntry (
				  DataDestination  theOutputDestination,
				  int              theSubsetToProduce,
				  String           theCxxNameLead,
				  boolean          isDistributedException,
				  boolean          theIsInsideClassDefinition )
    throws CompilationException
  {
	/* This data element is *ONLY* present for Distributed-Tasks. */
    if ( getIsDistributed() == false )
      return;

	/* INLINE is *NOT* compatible with this static attribute...      */
	/* (And, unfortunately, using a static atribute *IS* necessary.) */
    if ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE )
    {
      System.err.println (
	"[DataTaskDefinition:generateCxxDistributedRegistryEntry]  "
	+ "Error:  Inlined-C++ translation is *NOT* compatible with "
	+ "Distributed Tasks." );
      throw new CompilationException (
	"[DataTaskDefinition:generateCxxDistributedRegistryEntry]  "
	+ "Error:  Inlined-C++ translation is *NOT* compatible with "
	+ "Distributed Tasks." );
    }


       /* Generate the Exception_Creator function */
    if ( isDistributedException && !theIsInsideClassDefinition) {
      generateCxxDistributedExceptionCreator( theOutputDestination );
    }

	/* Indent us if we are inside a class definition. */
    if ( theIsInsideClassDefinition )
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	/* static  _TDL_DistributedRegistryEntry */

    if ( theIsInsideClassDefinition )
      theOutputDestination . write ( "static " );
    else
      theOutputDestination . write ( "/*static*/ " );

    theOutputDestination
      . write ( isDistributedException
		? DataComponent.CXX_DISTRIBUTED_EXCEPTION_REGISTRY_TYPE
		: DataComponent.CXX_DISTRIBUTED_REGISTRY_TYPE );

    theOutputDestination . write ( " " );


	/* The "class::", if outside the class definition. */
    if ( theIsInsideClassDefinition == false )
    {
      getTaskScope() . writeScope  ( theOutputDestination );
      theOutputDestination . write ( theCxxNameLead       );
      theOutputDestination . write ( getTaskName()        );
      writeUniqueIdStringIfNeeded  ( theOutputDestination );
      theOutputDestination . write ( "::" );
    }

	/* _TDL_thisDistributedRegistryEntry */
    theOutputDestination
      . write ( DataComponent.CXX_DISTRIBUTED_REGISTRY_NAME );


    if ( theIsInsideClassDefinition )
    {
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( " ;\n" );
    }
    else
    {
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( "\n( " );
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	/* First argument:  task name */
      theOutputDestination . write ( "\"" );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( "\",\n" );

      if ( isDistributedException ) 
      {
	/* Second argument: Distributed-remote format-information string. *
	 * (Checking for required #defines first...)                     */
	writeDistributedMacroRequirements ( theOutputDestination );
	theOutputDestination . setIsConcatenatingStrings ( true );
	theOutputDestination
	    . write ( getDistributedTaskFormatString ( theSubsetToProduce) );
	theOutputDestination . setIsConcatenatingStrings ( false );
	theOutputDestination . write ( ",\n" );
        
	/* Third argument:  Creator function */
	theOutputDestination . write ( DataComponent.CXX_CREATOR_FUNCTION_LEAD);
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( "\n" );
      }
      else
      {
	/* Second argument:  overloaded task name index */
	theOutputDestination . write ( "\"" );
	theOutputDestination . write ( getUniqueIdString() );
	theOutputDestination . write ( "\",\n" );

	/* Third argument:  Allocate function */
	getTaskScope() . writeScope ( theOutputDestination );
	theOutputDestination . write ( DataComponent.CXX_ALLOCATE_FUNCTION_LEAD);
	theOutputDestination . write ( getTaskName() );
	writeUniqueIdStringIfNeeded  ( theOutputDestination );
	theOutputDestination . write ( ",\n" );

	/* Fourth argument: Distributed-remote create-action function */
	getTaskScope() . writeScope  ( theOutputDestination );
	theOutputDestination . write (
	    DataComponent.CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_FUNCTION_LEAD );
	theOutputDestination . write ( getTaskName() );
	writeUniqueIdStringIfNeeded  ( theOutputDestination );
	theOutputDestination . write ( ",\n" );

	/* Fifth argument: Distributed-remote format-information string. *
	 * (Checking for required #defines first...)                     */
	writeDistributedMacroRequirements ( theOutputDestination );
	theOutputDestination . setIsConcatenatingStrings ( true );
	theOutputDestination
	    . write ( getDistributedTaskFormatString ( theSubsetToProduce) );
	theOutputDestination . setIsConcatenatingStrings ( false );
	theOutputDestination . write ( "\n" );
      }

      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( ");\n\n\n" );
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
    }
  }



	/* Internal routine to generateCxx */
	/* Generate the execute/resume method header/code. */
  protected void generateCxxExecuteOrResume (
			       DataDestination    theOutputDestination,
			       int                theSubsetToProduce,
			       DataVector         theNonUniqueNames,
			       String             theCxxNameLead,
			       boolean            theAllowResumeTasks,
			       DataTaskDefinition theMasterDataTaskDefinition )
  {
    int     i, taskArgumentSubset, tmpIndent;
    String  handleManagerDeclarationString;

    if ( hasExecuteMethod() == false )
      return;

		/***************************/
		/* Generate Execute/Resume */
		/***************************/

	  /* Write the beginning of the header statement... */
    if ( shouldDoCxxHeaderClassFor( theSubsetToProduce, theAllowResumeTasks ) )
    {
      if ( getTaskType() != DataTaskDefinition.RESUME_TASK )
      {
	theOutputDestination . write ( "virtual " );
	theOutputDestination . write ( DataComponent.CXX_EXECUTE_RETURN_VALUE);
      }
      else
      {
	theOutputDestination . write ( DataComponent.CXX_RESUME_RETURN_VALUE );
      }
      theOutputDestination . write ( " " );
    }

	/* Write the beginning of the trailer statement... */
    if ( shouldDoCxxCodeFor ( theSubsetToProduce ) )
    {
      if ( getTaskType() != DataTaskDefinition.RESUME_TASK )
      {
	theOutputDestination . write ( "/*virtual*/ " );
	theOutputDestination . write ( DataComponent.CXX_EXECUTE_RETURN_VALUE);
      }
      else
      {
	theOutputDestination . write ( DataComponent.CXX_RESUME_RETURN_VALUE );
      }
      theOutputDestination . write ( "\n" );
      getTaskScope() . writeScope ( theOutputDestination );
      theOutputDestination . write ( theCxxNameLead );
      theOutputDestination . write ( getTaskName() );
      writeUniqueIdStringIfNeeded  ( theOutputDestination );
      theOutputDestination . write ( "::" );
    }


    if (   shouldDoCxxHeaderClassFor ( theSubsetToProduce, theAllowResumeTasks)
	|| shouldDoCxxCodeFor        ( theSubsetToProduce ) )
    {
	/* Write the statement name & args... */
      if ( getTaskType() != DataTaskDefinition.RESUME_TASK )
      {
	theOutputDestination . write ( DataComponent.CXX_EXECUTE_METHOD );
      }
      else
      {
	theOutputDestination . write ( DataComponent.CXX_RESUME_NAME );
	theOutputDestination . write ( " ( " );

	   /* Indent remaining args nicely. */
	tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	   /* Deal with #line macros. */
	if ( getTaskArgumentCount() > 0 )
	  theOutputDestination . setUsingTdlFileName ( true );

	for ( i=0;  i < getTaskArgumentCount();  i++ )
	{
	  theOutputDestination . makeNextLineNumber (
				     getTaskArgument ( i ) . getLineNumber() );
	  if ( i > 0 )
	    theOutputDestination . write ( ",\n" );
	  else if ( theOutputDestination . getEnableLineMacros() )
	    theOutputDestination . write ( "\n" );

	  theOutputDestination . setStripLeadingWhitespace();

	    /* Comment out default argument unless we are in the header... */
	  if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == true )
	    taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_EQUALS;
	  else
	    taskArgumentSubset=DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS;

	  getTaskArgument ( i ) . generate ( theOutputDestination,
					     taskArgumentSubset   );
	}

	   /* Deal with #line macros. */
	if ( getTaskArgumentCount() > 0 )
	  theOutputDestination . setUsingTdlFileName ( false );

        /* Deal with #line macro newline down below... */

	theOutputDestination . write ( " )" );

	  /* Stop indenting the args. */
	theOutputDestination . removeIndent ( tmpIndent );
      }



      if (   ( theSubsetToProduce == DataComponent.CXX_CODE              )
	  || ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE      )
	  || ( theSubsetToProduce == DataComponent.CXX_CODE_NO_FUNCTIONS ) )
      {
		/* Add in our Handle-Manager code. */
		/* Using DataComponentPlaceholder to deal with #line numbers.*/
	getTaskBody() . addSubcomponentAfterOpenBrace (
	   new DataComponentPlaceholder ( this,
					  theNonUniqueNames,
					  getCachedStatementsVector(),
					  getCachedOnAgentHashtable()  ) );


		/* Add in our arguments-declaration code */
		/* Note: Also add _TDL_MARKUSED() for RESUME tasks. */
		/* Using DataComponentPlaceholder to deal with #line numbers.*/
	getTaskBody() . addSubcomponentAfterOpenBrace(
			 new DataComponentPlaceholder (
			   theMasterDataTaskDefinition,
			   getTaskType() == DataTaskDefinition.RESUME_TASK ) );


		/* Add in our _TDL_ENCLOSING_TASK declaration for RESUMEs */
	if ( getTaskType() == DataTaskDefinition.RESUME_TASK )
	  getTaskBody() . addSubcomponentAfterOpenBrace (
	     new DataComponentPlaceholder (
	       DataComponent.CXX_RESUME_TDL_ENCLOSING_TASK_DECLARATION ) );


		/*Add in our Task's NodeClassType verification code as needed*/
	if (   ( getTaskType() == DataTaskDefinition.GOAL_TASK    )
	    || ( getTaskType() == DataTaskDefinition.COMMAND_TASK )
	    || ( getTaskType() == DataTaskDefinition.MONITOR_TASK ) )
	  getTaskBody() . addSubcomponentAfterOpenBrace (
	     new DataComponentPlaceholder (
		   DataComponent.CXX_VERIFY_NODE_CLASS_TYPE
		   + " ( " 
		   + DataComponent.CXX_ENCLOSING_TASK_REF
		   + ", "
		   + ( (getTaskType() == DataTaskDefinition.GOAL_TASK)
		      ? DataComponent.CXX_TCM_GOAL
		      : ( (getTaskType() == DataTaskDefinition.COMMAND_TASK)
			 ? DataComponent.CXX_TCM_COMMAND
			 : DataComponent.CXX_TCM_MONITOR ) )
		   + ", \""
		   + getTaskTypeString() + " " + getTaskName() + "\" );\n" ) );


		/* Add in our Task's Distributed/Nondistributed *
		 * allocation function verification as needed.  */
	if (   ( getTaskType() == DataTaskDefinition.GOAL_TASK    )
	    || ( getTaskType() == DataTaskDefinition.COMMAND_TASK )
	    || ( getTaskType() == DataTaskDefinition.MONITOR_TASK ) )
	  getTaskBody() . addSubcomponentAfterOpenBrace (
	     new DataComponentPlaceholder (
		   DataComponent.CXX_VERIFY_NODE_ALLOCATION_FUNCTION
		   + " (\n       "
		   + DataComponent.CXX_ENCLOSING_TASK_REF
		   + ",\n       "
  + ( ( getIsDistributed() == false )
     ? DataComponent.CXX_TDL_ALLOCATION_TYPE_LOCAL_NONDISTRIBUTED_ONLY
     : ( ( isCxxDistributedOnlySubset ( theSubsetToProduce ) )
	? DataComponent.CXX_TDL_ALLOCATION_TYPE_DISTRIBUTED_ONLY
	: DataComponent.CXX_TDL_ALLOCATION_TYPE_EITHER_LOCAL_OR_DISTRIBUTED ) )
		   + ",\n       \""
		   + getTaskTypeString() + " " + getTaskName() + "\" );" ) );
	

		/* If we fall off the bottom of the Task, *
		 * assume we completed successfully.      */
	getTaskBody() . addSubcomponentBeforeCloseBrace ( 
	     new DataComponentPlaceholder (
			    "\n"
			  + DataComponent.CXX_TCM_TASK_COMPLETED_SUCCESSFULLY
			  + " ( "
		          + DataComponent.CXX_ENCLOSING_TASK_REF
		          + " );\n" ) );


	  /* Write the Task code */
	getTaskBody() . generate ( theOutputDestination, theSubsetToProduce );
	  /* Reset #line numbers back to Cxx file. */
	theOutputDestination . setUsingTdlFileName ( false );
	theOutputDestination . write ( "\n\n" );

		/* Remove the successful-completion, RESUME enclosing-task
		 * declaration, arguments-declaration, and Handle-Manager
		 * code strings...
		 */
	getTaskBody() . removeSubcomponentBeforeCloseBrace();

	if (   ( getTaskType() == DataTaskDefinition.GOAL_TASK    )
	    || ( getTaskType() == DataTaskDefinition.COMMAND_TASK )
	    || ( getTaskType() == DataTaskDefinition.MONITOR_TASK )
	    || ( getTaskType() == DataTaskDefinition.HANDLER_TASK ) )
	  getTaskBody() . removeSubcomponentAfterOpenBrace();

	if (   ( getTaskType() == DataTaskDefinition.GOAL_TASK    )
	    || ( getTaskType() == DataTaskDefinition.COMMAND_TASK )
	    || ( getTaskType() == DataTaskDefinition.MONITOR_TASK ) )
	  getTaskBody() . removeSubcomponentAfterOpenBrace();

	if ( getTaskType() == DataTaskDefinition.RESUME_TASK )
	  getTaskBody() . removeSubcomponentAfterOpenBrace();

	getTaskBody() . removeSubcomponentAfterOpenBrace();
	getTaskBody() . removeSubcomponentAfterOpenBrace();
      }
      else /* Write a empty execute() statement body */
      {
	theOutputDestination . write ( " ;\n\n" );
      }

    } /* IF ( shouldDoCxxHeaderClassFor || shouldDoCxxCodeFor ) */
  }




	/* Internal routine to generateCxx */
	/* Generate the external/inlined Resume function. */
  protected void generateCxxResumeFunction (
			       DataDestination    theOutputDestination,
			       int                theSubsetToProduce,
			       String             theCxxNameLead )
  {
    int  i, taskArgumentSubset, tmpIndent;

    if ( getTaskType() != DataTaskDefinition.RESUME_TASK )
      return;

    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE )
	     || ( theSubsetToProduce
		              == DataComponent.CXX_HEADER_INLINED_FUNCTIONS ) )
    {
      theOutputDestination . write ( "inline " );
    }

    theOutputDestination
      . write ( DataComponent.CXX_RESUME_FUNCTION_RETURN_VALUE );
    theOutputDestination . write ( "\n" );
    theOutputDestination . write ( DataComponent.CXX_RESUME_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	   /* Indent remaining args nicely. */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );

    theOutputDestination . write ( DataComponent.CXX_RESUME_FUNCTION_ARGUMENT);

	   /* Deal with #line macros. */
    if ( getTaskArgumentCount() > 0 )
      theOutputDestination . setUsingTdlFileName ( true );

    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
      theOutputDestination . makeNextLineNumber (
				     getTaskArgument ( i ) . getLineNumber() );
      theOutputDestination . write ( ",\n" );
      theOutputDestination . setStripLeadingWhitespace();

	    /* Comment out default argument unless we are in the header... */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == true )
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_EQUALS;
      else
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS;

      getTaskArgument ( i ) . generate ( theOutputDestination,
					 taskArgumentSubset   );
    }

	   /* Deal with #line macros. */
    if ( getTaskArgumentCount() > 0 )
      theOutputDestination . setUsingTdlFileName ( false );

        /* Deal with #line macro newline down below... */

    theOutputDestination . write ( " )" );

	  /* Stop indenting the args. */
    theOutputDestination . removeIndent ( tmpIndent );


    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( " ;\n\n" );
    }
    else
    {
      theOutputDestination . write ( "\n{" );
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( "\n" );
      theOutputDestination
	. write ( DataComponent.CXX_TDL_RESUME_INVOCATION_MACRO );
      theOutputDestination . write ( " ( " );
      tmpIndent = theOutputDestination . indentToCurrentColumn ( );      
      theOutputDestination . write ( theCxxNameLead );
      theOutputDestination . write ( getTaskName() );
      writeUniqueIdStringIfNeeded  ( theOutputDestination );
      theOutputDestination . write ( ",\n" );
      theOutputDestination . write ( DataComponent.CXX_ENCLOSING_TASK_REF );
      theOutputDestination . write ( " ) ( " );

	   /* Indent remaining args nicely. */
      tmpIndent += theOutputDestination . indentToCurrentColumn ( );

      for ( i=0;  i < getTaskArgumentCount();  i++ )
      {
	if ( i > 0 )
	  theOutputDestination . write ( ",\n" );
	theOutputDestination
	  . write ( getTaskArgument ( i ) . getArgumentName() );
      }

	  /* Stop indenting the args. */
      theOutputDestination . removeIndent ( tmpIndent );
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

      theOutputDestination . write ( " );\n}\n\n" );
    }
  }



	/* Internal routine to generateCxxAllocate(). *
	 * Assumes isDistributedAndNormal == true     */
  private void generateCxxAllocate_generateConstraintConditional (
					DataDestination  theOutputDestination )
  {
    theOutputDestination
      . write ( "\n  // Either Remote-side Distributed Task  OR  " );
    theOutputDestination
      . write ( "Non-Distributed Task scenario.\n" );
    theOutputDestination . write ( "if ( " );
    theOutputDestination
      . write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_NAME );
    theOutputDestination . write ( " == (" );
    theOutputDestination
      . write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_TYPE );
    theOutputDestination . write ( ")NULL )\n{\n" );


    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
  }

	/* Internal routine to generateCxx */
  protected void generateCxxAllocate (
			     DataDestination  theOutputDestination,
			     int              theSubsetToProduce    )
  {
    int     indent, i;
    boolean addedNewLine;
    boolean needsDistributedConditional,
            hasDistributedConditional;
    boolean isDistributedAndNormal = false;
    boolean isNormalOnly           = false;
    boolean isDistributedOnly      = false;


	/* Figure out what we are doing... */
    if ( getIsDistributed() == true )
    {
      if ( isCxxDistributedOnlySubset ( theSubsetToProduce ) )
	isDistributedOnly = true;
      else
	isDistributedAndNormal = true;
    }
    else
      isNormalOnly = true;


    if(  ( getTaskScope() . hasScope() == false )
       &&(   ( theSubsetToProduce == DataComponent.CXX_HEADER                 )
	  || ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER            )
	  || ( theSubsetToProduce == DataComponent.CXX_HEADER_DISTRIBUTED_ONLY)
	  || ( getIsExtern()      == true                                     )
	  ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE      )
	     || ( theSubsetToProduce
		       == DataComponent.CXX_HEADER_INLINED_FUNCTIONS        )
	     || ( theSubsetToProduce
		       == DataComponent.CXX_HEADER_INLINED_DISTRIBUTED_ONLY ) )
    {
      theOutputDestination . write ( "inline " );
    }

    if ( getIsStatic() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.STATIC );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

    if ( getIsVirtual() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.VIRTUAL );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

	/* "TCM_Task_Tree_Ref " [scope] "_TDL_Allocate_" getTaskName " ( " */
    theOutputDestination . write ( DataComponent.CXX_ALLOCATE_RETURN_VALUE  );
    theOutputDestination . write ( "\n" );
	/* Only write scope outside of classes, in the code generation. */
    if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
      getTaskScope() . writeScope ( theOutputDestination );
    theOutputDestination . write ( DataComponent.CXX_ALLOCATE_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    writeUniqueIdStringIfNeeded  ( theOutputDestination );
    theOutputDestination . write ( " ( " );

    indent = theOutputDestination . indentToCurrentColumn ( );

	/* First argument:  const char * theName = (const char *)NULL */
    theOutputDestination . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_TYPE );
    theOutputDestination . write ( "  " );
    theOutputDestination . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_NAME );
	/* If we are doing isDistributedAndNormal, lets line this up nicely. */
    if ( isDistributedAndNormal )
      theOutputDestination . write ( "      " );

	/* Skip the default argument for distributed-only case,        *
	 * since the second argument CANNOT be defaulted in that case. */
    if ( isDistributedOnly == false )
    {
	/* Comment out default argument unless we are in the header. */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
	theOutputDestination . write( " /*" );

      theOutputDestination . write ( " = (" );
      theOutputDestination . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_TYPE);
      theOutputDestination . write ( ")NULL" );

	/* Comment out default argument unless we are in the header. */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
	theOutputDestination . write ( " */" );
    } /* if ( isDistributedOnly == false ) */



	/* The Second argument is present only for DISTRIBUTED/NORMAL
	 * and DISTRIBUTED-ONLY tasks.
	 * The Third argument  is present only for DISTRIBUTED/NORMAL Tasks.
	 */
    if ( isDistributedOnly || isDistributedAndNormal )
    {
      theOutputDestination . write ( ",\n" );


	/* Second argument:  const char * theAgentName = (const char *)NULL */
      theOutputDestination
	. write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_TYPE );
      theOutputDestination . write ( "  " );
      theOutputDestination
	. write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_NAME );
      
	/* Skip the default argument for distributed-only case,        *
	 * since the second argument CANNOT be defaulted in that case. */
      if ( isDistributedOnly == false )
      {
	theOutputDestination . write ( " " );

	  /* Comment out default argument unless we are in the header. */
	if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
	  theOutputDestination . write ( " /*" );

	theOutputDestination . write ( " = (" );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_TYPE );
	theOutputDestination . write ( ")NULL" );

	  /* Comment out default argument unless we are in the header. */
	if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
	  theOutputDestination . write ( " */" );
      } /* if ( isDistributedOnly == false ) */




	/* The THIRD argument is not present on DISTRIBUTED-ONLY invocation.
	 * It is only present on DISTRIBUTED/NORMAL Tasks.
	 */
      if ( isDistributedAndNormal )
      {
	theOutputDestination . write( ",\n" );


	  /* Third argument:  BOOLEAN      theIsDistributedRemoteSide = FALSE*/
	theOutputDestination . write (
	      DataComponent.CXX_ALLOCATE_IS_DISTRIBUTED_REMOTE_ARGUMENT_TYPE );
	theOutputDestination . write ( " " );
	theOutputDestination . write (
	      DataComponent.CXX_ALLOCATE_IS_DISTRIBUTED_REMOTE_ARGUMENT_NAME );

	 /* Comment out default argument unless we are in the header... */
	if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
	  theOutputDestination . write( " /*" );

	theOutputDestination . write ( " = FALSE" );

	  /* Comment out default argument unless we are in the header... */
	if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
	  theOutputDestination . write( " */" );

      } /* IF ( isDistributedAndNormal )  Do Third argument */

	/* The Second/Third arguments are only present for DISTRIBUTED tasks */
    } /* if ( isDistributedOnly || isDistributedAndNormal ) */


    theOutputDestination . write( " )" );
    theOutputDestination . removeIndent ( indent );




    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER                 )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER            )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER_DISTRIBUTED_ONLY)
	|| ( getIsExtern()      == true                                     ) )
    {
      theOutputDestination . write( " ;\n" );
    }
    else
    {
      theOutputDestination . write ( "\n{\n" );
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	/* At present, we know if we are local-nondistributed,
	 * local-distributed, and remote-distributed.  But, we don't need to
	 * distinguish between local-nondistributed and remote-distributed.
	 *
	 * We do, however, need to have the different Allocation function
	 * signatures, and we may need this information at some future date.
	 */
      if ( isDistributedAndNormal )
      {
	  /* "_TDL_MARKUSED ( theIsDistributedRemoteSide );" */
	theOutputDestination
	  . write ( DataComponent.CXX_MARK_AS_USED_TO_COMPILER );
	theOutputDestination . write ( " ( " );
	theOutputDestination . write (
	      DataComponent.CXX_ALLOCATE_IS_DISTRIBUTED_REMOTE_ARGUMENT_NAME );
	theOutputDestination . write ( " );\n\n" );
      }

	/* "TCM_Task_Tree_Ref  allocatedNode;" */
      theOutputDestination . write ( DataComponent.CXX_ALLOCATE_RETURN_VALUE );
      theOutputDestination . write ( "  " );
      theOutputDestination . write ( DataComponent.CXX_ALLOCATED_NODE_NAME );


      if ( isDistributedAndNormal )
      {
	theOutputDestination . write ( ";\n\n" );

		/* if ( theAgentName == (const char *)NULL ) */
	theOutputDestination . write ( "if ( " );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_NAME );
	theOutputDestination . write ( " == (" );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_TYPE );
	theOutputDestination . write ( ")NULL )\n" );

	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
	
		/* allocatedNode = ... */
	theOutputDestination . write ( DataComponent.CXX_ALLOCATED_NODE_NAME );
	theOutputDestination . write ( " = " );

	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

      } /* if ( isDistributedAndNormal ) */

      else /* E.g. We are doing isNormalOnly or isDistributedOnly */
      {
	theOutputDestination . write ( "\n  = " );
      }


      if ( isNormalOnly || isDistributedAndNormal )
      {
	switch ( getTaskType() )
	{
	  case DataTaskDefinition.GOAL_TASK:
	    theOutputDestination
	      . write ( DataComponent.CXX_TCM_ALLOCATE_GOAL_FUNCTION );
	    break;

	  case DataTaskDefinition.COMMAND_TASK:
	    theOutputDestination
	      . write ( DataComponent.CXX_TCM_ALLOCATE_COMMAND_FUNCTION );
	    break;

	  case DataTaskDefinition.MONITOR_TASK:
	    theOutputDestination
	      . write ( DataComponent.CXX_TCM_ALLOCATE_MONITOR_FUNCTION );
	    break;

	  default:
	    throw new CompilationException (
		    "Error:  No KNOWN TCM allocate function for Task type \""
		    + getTaskTypeString() + "\"." );
	}


	  /* Start arguments */
	theOutputDestination . write ( " ( " );
	indent = theOutputDestination . indentToCurrentColumn ( );

	  /* write: "scope::taskName",\n */
	theOutputDestination . write ( "\"" );
	getTaskScope() . writeScope ( theOutputDestination );
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( "\",\n" );

	  /* write: (   ( theName == (const char *)NULL ) *
	   *          ? "scope::taskname" : theName )            */
	theOutputDestination . write ( "( ( " );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_NAME );
	theOutputDestination . write ( " == (" );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_TYPE );
	theOutputDestination . write ( ")NULL )\n ? \"" );
	getTaskScope() . writeScope ( theOutputDestination );
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( "\" : " );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_NAME );
	theOutputDestination . write ( " ) );\n" );
	theOutputDestination . removeIndent ( indent );

      } /* if ( isNormalOnly || isDistributedAndNormal ) */


      if ( isDistributedAndNormal )
      {
	    /* Write:  else allocatedNode = */
	theOutputDestination . write ( "else\n" );

	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
	theOutputDestination . write ( DataComponent.CXX_ALLOCATED_NODE_NAME );
	theOutputDestination . write ( " = " );
	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      } /* If ( isDistributedAndNormal ) */


      if ( isDistributedAndNormal || isDistributedOnly )
      {
	    /* Write: TCM_AllocateDistributedNode */
	theOutputDestination
	  . write ( DataComponent.CXX_TCM_ALLOCATE_DISTRIBUTED_FUNCTION );

	    /* Write ( theAgentName,
	     *         "scope::taskname",
	     *         ( ( theName == (const char *)NULL )
	     *          ? "scope::taskname" : theName ),
	     *         "<overloaded-task-name-index>" );
	     */
	theOutputDestination . write ( " ( " );
	indent = theOutputDestination . indentToCurrentColumn ( );
	theOutputDestination . write ( "" );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_AGENT_ARGUMENT_NAME );
	theOutputDestination . write ( ",\n" );

	theOutputDestination . write ( "\"" );
	getTaskScope() . writeScope ( theOutputDestination );
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( "\",\n" );

	theOutputDestination . write ( "( ( " );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_NAME );
	theOutputDestination . write ( " == (" );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_TYPE );
	theOutputDestination . write ( ")NULL )\n ? \"" );
	getTaskScope() . writeScope ( theOutputDestination );
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( "\" : " );
	theOutputDestination
	  . write ( DataComponent.CXX_ALLOCATE_ARGUMENT_NAME );
	theOutputDestination . write ( " ),\n" );

	theOutputDestination . write ( "\"" );
	theOutputDestination . write ( getUniqueIdString() );
	theOutputDestination . write ( "\" );\n" );
	theOutputDestination . removeIndent ( indent );

      } /* if ( isDistributedAndNormal || isDistributedOnly ) */



	/* All constraints are now dealt with in the create-action function.
	 * This resolves issues relating to task-name overloading.
	 */


	/*
	 * Cache the allocation-function type here (for later verification).
	 */
      theOutputDestination . write ( "\n" );
      theOutputDestination
	. write ( DataComponent.CXX_TDL_CACHE_NODE_ALLOCATION_FUNCTION_TYPE );
      theOutputDestination . write ( " (\n       " );

      indent = theOutputDestination . indentToCurrentColumn ( );
      theOutputDestination . write ( DataComponent.CXX_ALLOCATED_NODE_NAME );
      theOutputDestination . write ( ",\n" );

      if ( isNormalOnly )
	theOutputDestination . write (
	  DataComponent.CXX_TDL_ALLOCATION_TYPE_LOCAL_NONDISTRIBUTED_ONLY );

      else if ( isDistributedAndNormal )
	theOutputDestination . write (
	  DataComponent.CXX_TDL_ALLOCATION_TYPE_EITHER_LOCAL_OR_DISTRIBUTED );

      else if ( isDistributedOnly )
	theOutputDestination . write (
	  DataComponent.CXX_TDL_ALLOCATION_TYPE_DISTRIBUTED_ONLY );

      else
	theOutputDestination . write (
	  DataComponent.CXX_TDL_ALLOCATION_TYPE_UNKNOWN );

      theOutputDestination . write ( " );\n" );
      theOutputDestination . removeIndent ( indent );



	/* Write "return allocatedNode;" */
      theOutputDestination . write ( "\nreturn " );
      theOutputDestination . write ( DataComponent.CXX_ALLOCATED_NODE_NAME );
      theOutputDestination . write ( ";\n" );

	/* End this function... */
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( "}\n" );      

    } /* IF ( not writing allocate function body ) ELSE ... */


	/* Write macro for overloading... */
    writeOverloadingBackwardCompatibilityIfdefIfNeeded (
      theOutputDestination,
      DataComponent.CXX_ALLOCATE_FUNCTION_LEAD + getTaskName() );

    theOutputDestination . write ( "\n\n" );

  } /* void generateCxxAllocate ( ... ) */







	/* Internal routine to generateCxx */
  protected void generateCxxCreateAction (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce )
    /* throws CompilationException */
  {
    int             i, taskArgumentSubset, tmpIndent;
    DataConstraint  dataConstraint;
    boolean         addedNewLine;
    boolean         needsDistributedConditional,
                    hasDistributedConditional;
    boolean         isDistributedAndNormal = false;
    boolean         isNormalOnly           = false;
    boolean         isDistributedOnly      = false;
    final String    errorLocation = "\"["
                                 + getTaskScope() . getAllScopeStrings()
				 + DataComponent.CXX_CREATEACTION_FUNCTION_LEAD
				 + getTaskName() + ":\'"
				 + DataComponent.CXX_CREATEACTION_VARIABLE_NAME
				 + "\']\"";


	/* Figure out what we are doing... */
    if ( getIsDistributed() == true )
    {
      if ( isCxxDistributedOnlySubset ( theSubsetToProduce ) )
	isDistributedOnly = true;
      else
	isDistributedAndNormal = true;
    }
    else
      isNormalOnly = true;


    if(  ( getTaskScope() . hasScope() == false )
       &&(   ( theSubsetToProduce == DataComponent.CXX_HEADER                 )
	  || ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER            )
	  || ( theSubsetToProduce == DataComponent.CXX_HEADER_DISTRIBUTED_ONLY)
	  || ( getIsExtern()      == true                                     )
	  ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE      )
	     || ( theSubsetToProduce
		       == DataComponent.CXX_HEADER_INLINED_FUNCTIONS        )
	     || ( theSubsetToProduce
		       == DataComponent.CXX_HEADER_INLINED_DISTRIBUTED_ONLY ) )
    {
      theOutputDestination . write ( "inline " );
    }

    if ( getIsStatic() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.STATIC );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

    if ( getIsVirtual() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.VIRTUAL );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

      /* "_TDL_ActionOrVoid " [scope] "_TDL_CreateAction_" getTaskName " ( " */
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_RETURN_VALUE  );
    theOutputDestination . write ( "\n" );
	/* Only write scope outside of classes, in the code generation. */
    if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
      getTaskScope() . writeScope ( theOutputDestination );
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	/* Lets indent all the arguments nicely... */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	/* Write the Task-To-Configure  Task-Ref argument */
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT_TYPE );
    theOutputDestination . write ( " " );
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );
    theOutputDestination . write ( ",\n" );

	/* Write the delayed-allocation argument */
    if ( isNormalOnly )
      theOutputDestination . write (
	DataComponent.CXX_LOCAL_NONDISTRIBUTED_ONLY_DELAYED_ALLOCATION );
    else if ( isDistributedAndNormal )
      theOutputDestination . write (
	DataComponent.CXX_EITHER_LOCAL_OR_DISTRIBUTED_DELAYED_ALLOCATION );
    else if ( isDistributedOnly )
      theOutputDestination . write (
	DataComponent.CXX_DISTRIBUTED_ONLY_DELAYED_ALLOCATION );
    else
      throw new CompilationException (
	"Internal Error:  DataTaskDefinition object is neither "
	+ "isNormalOnly, isDistributedAndNormal, nor isDistributedOnly." );
    theOutputDestination . write ( "\n      " );
    theOutputDestination
      . write ( DataComponent.CXX_DELAYED_ALLOCATION_OBJECT );


	/* Write Task Arguments */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
	    /* Deal with #line macros */
      theOutputDestination . setUsingTdlFileName ( true );
      theOutputDestination
	. makeNextLineNumber ( getTaskArgument ( i ) . getLineNumber() );

      theOutputDestination . write ( ",\n" );
      theOutputDestination . setStripLeadingWhitespace();

	/* Comment out default argument unless we are in the header... */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == true )
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_EQUALS;
      else
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS;

      getTaskArgument ( i ). generate ( theOutputDestination,
					taskArgumentSubset   );
    }
      	/* Deal with #line macros */
    theOutputDestination . setUsingTdlFileName ( false );

    theOutputDestination . write ( " )" );

	/* Lets stop indenting the arguments... */
    theOutputDestination . removeIndent ( tmpIndent );




	/* Are we NOT writing the body??? */
    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER                 )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER            )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER_DISTRIBUTED_ONLY)
	|| ( getIsExtern()      == true                                     ) )
    {
      theOutputDestination . write ( " ;\n\n" );
      return;
    }

	/* Otherwise, write the body... */

    theOutputDestination . write ( "\n{\n" );
    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );



	/* Deal with delayed allocation here. */
    theOutputDestination
      . write ( DataComponent.CXX_TDL_PROCESS_DELAYED_ALLOCATION_CAN_ABORT );
    theOutputDestination . write ( " (\n" );
	  /* Indent these arguments... */
    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

    theOutputDestination . write ( "\"" );
    getTaskScope() . writeScope ( theOutputDestination );
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( "\",\n" );

    theOutputDestination
      . write ( DataComponent.CXX_DELAYED_ALLOCATION_OBJECT );
    theOutputDestination . write ( ",\n" );

    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );
    theOutputDestination . write ( ",\n" );

    theOutputDestination . write ( DataComponent.CXX_ALLOCATE_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    writeUniqueIdStringIfNeeded  ( theOutputDestination );
    theOutputDestination . write ( " ( " );
	  /* Indent these arguments... */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );

    theOutputDestination
      . write ( DataComponent.CXX_DELAYED_ALLOCATION_OBJECT );
    theOutputDestination . write ( " . " );
    theOutputDestination
      . write ( DataComponent.CXX_DELAYED_ALLOCATION_GET_NAME_METHOD );

    if ( isDistributedAndNormal || isDistributedOnly )
    {
      theOutputDestination . write ( ",\n" );
      theOutputDestination
	. write ( DataComponent.CXX_DELAYED_ALLOCATION_OBJECT );
      theOutputDestination . write ( " . " );
      theOutputDestination
	. write ( DataComponent.CXX_DELAYED_ALLOCATION_GET_AGENT_NAME_METHOD );
    }

    if ( isDistributedAndNormal )
    {
      theOutputDestination . write ( ",\n" );
      theOutputDestination . write (
	DataComponent.CXX_DELAYED_ALLOCATION_IS_NOT_DISTRIBUTED_REMOTE_SIDE );
    }

    theOutputDestination . write ( " ) );\n\n" );
	/* Stop indenting the arguments */
    theOutputDestination . removeIndent ( tmpIndent );
    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );




	/* Create a Trivial-Task-Handler, thereby marking
	 * the task-argument as used (to avoid error messages),
	 * and also allowing the keyword THIS to be used.
	 *
	 * Note:  This is unnecessary for isDistributedOnly scenario.
	 */
    if ( isDistributedAndNormal || isNormalOnly )
    {
      theOutputDestination
	. write ( DataComponent.CXX_TDL_TRIVIAL_TASK_HANDLER_CLASS );
      theOutputDestination . write ( " " );
      theOutputDestination
	. write ( DataComponent.CXX_TDL_TASK_HANDLER_INSTANCE );
      theOutputDestination . write ( " ( " );

	  /* Indent these arguments... */
      tmpIndent = theOutputDestination . indentToCurrentColumn ( );

      theOutputDestination . write ( "\"" );
      getTaskScope() . writeScope ( theOutputDestination );
      theOutputDestination
	. write ( DataComponent.CXX_CREATEACTION_FUNCTION_LEAD );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( "\",\n" );
      theOutputDestination
	. write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );

	/* Lets stop indenting these arguments... */
      theOutputDestination . removeIndent ( tmpIndent );

      theOutputDestination . write ( " );\n\n" );
    }

    else /* E.g. isDistributedOnly */
    {
	/* Better mark this as USED... */
      theOutputDestination
	. write ( DataComponent.CXX_MARK_AS_USED_TO_COMPILER );
      theOutputDestination . write ( " ( " );
      theOutputDestination
	. write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );
      theOutputDestination . write ( " );\n\n" );
    }




	/* Create our value to return.
	 * Either a (_TDL_Action *) or a (void *) for local-distributed.
	 */
		/* "_TDL_ActionOrVoid  createdActionOrVoid;" */
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_RETURN_VALUE );
    theOutputDestination . write ( "  " );
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_VARIABLE_NAME );
    theOutputDestination . write ( ";\n" );


    if ( isDistributedAndNormal )
    {
	/* if (_TDL_DO_CHECK_IF_TASK_DISTRIBUTED (_TDL_ENCLOSING_TASK) == TRUE)
	 * {
	 */
      theOutputDestination . write ( "if ( " );
      theOutputDestination
	. write ( DataComponent.CXX_CHECK_IF_TASK_DISTRIBUTED );
      theOutputDestination . write ( " ( " );
      theOutputDestination
	. write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );
      theOutputDestination . write ( " ) == TRUE )\n{\n" );
      theOutputDestination . addIndent (DataComponent.STANDARD_INDENT);

    } /* if ( isDistributedAndNormal ) */


    if ( isDistributedAndNormal || isDistributedOnly )
    {
	/* If there are no arguments, there is no fancy struct. *
	 * Just a (void *)NULL value.                           */
      if ( getTaskArgumentCount() > 0 )
      {
	  /* _TDL_DISTRIBUTED_STRUCT and _TDL_DISTRIBUTED_STRUCT_POINTER
	   * declaration
	   */
	generateCxxDistributedStruct ( theOutputDestination );

	  /* _TDL_DISTRIBUTED_STRUCT_POINTER = new _TDL_DISTRIBUTED_STRUCT;
	   */
	theOutputDestination
	  . write ( DataComponent.CXX_DISTRIBUTED_STRUCT_POINTER );
	theOutputDestination . write ( " = " );
	theOutputDestination . write ( DataComponent.CXX_MALLOC_FUNCTION );
	theOutputDestination . write ( " ( " );
	theOutputDestination
	  . write ( DataComponent.CXX_DISTRIBUTED_STRUCT_NAME );
	theOutputDestination . write ( " );\n" );

	  /* _TDL_DISTRIBUTED_STRUCT_POINTER -> arg = arg
	   */
	for ( i=0;  i < getTaskArgumentCount();  i++ )
	{
	  theOutputDestination
	    . write ( DataComponent.CXX_DISTRIBUTED_STRUCT_POINTER );
	  theOutputDestination . write ( " -> " );

	  theOutputDestination . setStripLeadingWhitespace();
	  theOutputDestination
	    . write ( getTaskArgument ( i ) . getArgumentName() );
      
	  theOutputDestination . write ( " = " );

	  theOutputDestination . setStripLeadingWhitespace();
	  theOutputDestination
	    . write ( getTaskArgument ( i ) . getArgumentName() );

	  theOutputDestination . write ( ";\n" );
	}
      } /* if ( getTaskArgumentCount() > 0 ) */

	/* createdActionOrVoid
	 *   . setVoidPointer ( (void*) _TDL_DISTRIBUTED_STRUCT_POINTER,
	 *                      "<overloaded-task-name-index>" );
	 */
      theOutputDestination
	. write ( DataComponent.CXX_CREATEACTION_VARIABLE_NAME );
      theOutputDestination . addIndent (DataComponent.STANDARD_INDENT);
      theOutputDestination . write ( "\n. " );
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination
	. write ( DataComponent.CXX_CREATE_ACTION_OR_VOID_SET_VOID );
      theOutputDestination . write ( " ( " );

      tmpIndent = theOutputDestination . indentToCurrentColumn ( );
      theOutputDestination . write ( "(void *) " );
      if ( getTaskArgumentCount() > 0 )
	theOutputDestination
	  . write ( DataComponent.CXX_DISTRIBUTED_STRUCT_POINTER );
      else
	theOutputDestination . write ( "NULL" );

      theOutputDestination . write ( ",\n\"" );
      theOutputDestination . write ( getUniqueIdString() );
      theOutputDestination . write ( "\" );\n" );
      theOutputDestination . removeIndent ( tmpIndent );
    } /* if ( isDistributedAndNormal || isDistributedOnly ) */


    if ( isDistributedAndNormal )
    {
	/* }
         * else
	 * {
	 */
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( "}\nelse\n{\n" );
      theOutputDestination . addIndent (DataComponent.STANDARD_INDENT);
    } /* if ( isDistributedAndNormal ) */



    if ( isDistributedAndNormal || isNormalOnly )
    {
	/* createdActionOrVoid
	 *   . setActionPointer ( new _TDL_taskname ( args ) );
	 */
      theOutputDestination
	. write ( DataComponent.CXX_CREATEACTION_VARIABLE_NAME );
      theOutputDestination . addIndent (DataComponent.STANDARD_INDENT);
      theOutputDestination . write ( "\n. " );
      theOutputDestination
	. write ( DataComponent.CXX_CREATE_ACTION_OR_VOID_SET_ACTION );
      theOutputDestination . write ( " ( new " );

      theOutputDestination . write ( DataComponent.CXX_NAME_LEAD );
      theOutputDestination . write ( getTaskName() );
      writeUniqueIdStringIfNeeded  ( theOutputDestination );
      theOutputDestination . write ( " ( " );

	  /* Indent the arguments... */
      tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	  /* Write the task-argument-names as arguments to the new... */
      for ( i=0;  i < getTaskArgumentCount();  i++ )
      {
	if ( i > 0 )
	  theOutputDestination . write ( ",\n" );
	theOutputDestination . setStripLeadingWhitespace();
	theOutputDestination
	  . write ( getTaskArgument ( i ) . getArgumentName() );
      }

      theOutputDestination . write ( " ) );\n" );

	/* Lets stop indenting the new arguments... */
      theOutputDestination . removeIndent ( tmpIndent );
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

    } /* if ( isDistributedAndNormal || isNormalOnly ) */


    if ( isDistributedAndNormal )
    {
      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( "}\n" );
    } /* if ( isDistributedAndNormal ) */




        /* Write constraints */
    if (   (   isDistributedAndNormal
	    || isNormalOnly                                         )
	&& (   ( getTaskType() == DataTaskDefinition.GOAL_TASK    )
	    || ( getTaskType() == DataTaskDefinition.COMMAND_TASK )
	    || ( getTaskType() == DataTaskDefinition.MONITOR_TASK ) ) )
    {
	/* We only conditionalize the task-level constraints for *
	 * distributed tasks when we have such constraints.      */
      needsDistributedConditional = isDistributedAndNormal;
      hasDistributedConditional   = false;


      addedNewLine = false;
      for ( i=-2;  i < getConstraintsCount();  i++ )
      {
	if (   (   ( i == -2           )
		&& ( getIsPersistent() ) )
	    ||
	       (   ( i == -1           )
		&& ( getIsThreaded()   ) )
	    ||
	       (   ( i >= 0            )

			/* Skip constraints that are overriden... */
		&& ( DataConstraint.getIsConstraintOverriden (
						        getConstraint ( i ),
							getConstraints()    )
		       == false        )

			/* Skip the DISTRIBUTED_FORMAT constraints. *
			 * They are dealt with elsewhere in         *
			 * generateCxxDistributedRegistryEntry()    */
		&& ( getConstraint ( i ) .  getConstraintType()
		     != DataConstraint.DISTRIBUTED_FORMAT
		                       ) )
	    )
	{
		/* Space down a line for the first constraint. */
	  if ( addedNewLine == false )
	  {
	    theOutputDestination . write ( "\n" );
	    addedNewLine = true;
	  }

		/* Constraints, when applied at the Task-Level, with respect *
		 * to Distributed Tasks, must be invoked on the remote node. */
	  if ( needsDistributedConditional == true )
	  {
	    needsDistributedConditional = false;
	    hasDistributedConditional   = true;

	    theOutputDestination
	      . write ( "\n  // Either Remote-side Distributed Task  OR  " );
	    theOutputDestination
	      . write ( "Non-Distributed Task scenario.\n" );
	    theOutputDestination . write ( "if ( " );
	    theOutputDestination
	      . write ( DataComponent.CXX_CHECK_IF_TASK_DISTRIBUTED );
	    theOutputDestination . write ( " ( " );
	    theOutputDestination
	      . write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );
	    theOutputDestination . write ( " ) == FALSE )\n{" );
	    theOutputDestination . addIndent (DataComponent.STANDARD_INDENT);
	  } /* if ( isDistributedAndNormal ) */


		/* Deal with PERSISTENT Tasks */
	  switch ( i )
	  {
	    case -2:  /* getIsPersistent() */
	      theOutputDestination
		. write ( "\n       // Task is PERSISTENT.\n" );
	      theOutputDestination
		. write ( DataComponent.CXX_VERIFY_CONSTRAINT );
	      theOutputDestination . write ( " ( " );
	      tmpIndent = theOutputDestination . indentToCurrentColumn ( );
	      theOutputDestination . write ( errorLocation );
	      theOutputDestination . write ( ",\n\"" );
	      theOutputDestination
		. write ( DataComponent.CXX_TCM_SET_PERSISTENCE );
	      theOutputDestination . write ( "\",\n" );
	      theOutputDestination
		. write ( DataComponent.CXX_TCM_SET_PERSISTENCE );
	      theOutputDestination . write ( " ( " );
	      theOutputDestination
		. write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );
	      theOutputDestination . write ( ", TRUE ) );\n" );
	      theOutputDestination . removeIndent ( tmpIndent );
	      break;


	    case -1: /* getIsThreaded() */
	      theOutputDestination . write ("\n       // Task is THREADED.\n");
	      theOutputDestination
		. write (DataComponent.CXX_VERIFY_CONSTRAINT);
	      theOutputDestination . write ( " ( " );
	      tmpIndent = theOutputDestination . indentToCurrentColumn ( );
	      theOutputDestination . write ( errorLocation );
	      theOutputDestination . write ( ",\n\"" );
	      theOutputDestination
		. write ( DataComponent.CXX_TCM_SET_THREADED );
	      theOutputDestination . write ( "\",\n" );
	      theOutputDestination
		. write ( DataComponent.CXX_TCM_SET_THREADED );
	      theOutputDestination . write ( " ( " );
	      theOutputDestination
		. write ( DataComponent.CXX_CREATEACTION_TASK_ARGUMENT );
	      theOutputDestination . write ( ", TRUE ) );\n" );
	      theOutputDestination . removeIndent ( tmpIndent );
	      break;


	    default: /* ( i >= 0 ) */
		/* Some Constraints, such as on-terminate, *
		 * need to allocate secondary subtasks.    */
	      getConstraint ( i )
		. generateCxxExternalSubtaskDeclarations (
						       theOutputDestination,
						       true /*add newline*/ );

	      getConstraint ( i )
		. generateCxxTaskExternal (
				 theOutputDestination,
				 errorLocation,
				 DataComponent.CXX_CREATEACTION_TASK_ARGUMENT,
				 "",    /* NO return-value string */
				 false, /* NOT inside IF statement */
				 (    getTaskType()
				   == DataTaskDefinition.MONITOR_TASK ) );
	      break;

	  } /* switch ( i ) */
	} /* IF ( we should generate this constraint here ) */
      } /* for ( i=-2;  i < getConstraintsCount();  i++ ) */


	  /* Finish off the conditionalized distributed constraints */
      if ( hasDistributedConditional == true )
      {
	theOutputDestination . removeIndent(DataComponent.STANDARD_INDENT);
	theOutputDestination . write ( "}\n" );
      } /* if ( hasDistributedConditional == true ) */


    } /*IF ( DistributedAndNormal or NormalOnly && Goal/Command/Monitor Task)*/



	/* Return the value */
    theOutputDestination . write ( "\nreturn " );
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_VARIABLE_NAME );
    theOutputDestination . write ( ";\n" );


    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
    theOutputDestination . write ( "}\n\n\n" );      


  } /* void generateCxxCreateAction ( ... ) */





  protected void generateCxxCreateDistributedAction (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    int i, tmpIndent;

	/* This function is *ONLY* present for Distributed-Tasks. */
    if ( getIsDistributed() == false )
      return;

    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE )
	     || ( theSubsetToProduce
		              == DataComponent.CXX_HEADER_INLINED_FUNCTIONS ) )
    {
      theOutputDestination . write ( "inline " );
    }

    
    theOutputDestination . write (
	    DataComponent.CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_RETURN_VALUE  );
    theOutputDestination . write ( "\n" );
    theOutputDestination . write (
	    DataComponent.CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    writeUniqueIdStringIfNeeded  ( theOutputDestination );
    theOutputDestination . write ( " (\n" );

	/* Lets indent all the arguments nicely... */
    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT * 2 );

	/* Write the arguments.
	 * This signature must correspond EXACTLY to what is specified in TCM!
	 */
    for ( i=0;
	  i < DataComponent
	       . CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_FUNCTION_ARGUMENTS
	       . length;
	  i++ )
    {
      if ( i > 0 )
	theOutputDestination . write ( ",\n" );

      theOutputDestination . write (
	DataComponent
	  . CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_FUNCTION_ARGUMENTS [ i ] );
    }

    theOutputDestination . write ( " )" );

	/* Lets stop indenting the arguments... */
    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT * 2);



	/* Are we NOT writing the body??? */
    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( " ;\n\n" );
      return;
    }

	/* Otherwise, write the body... */

    theOutputDestination . write ( "\n{\n" );
    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );


	/* Mark these arguments as "USED" to the compiler... */
    for ( i=0;
	  i < DataComponent
	       . CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_FUNCTION_ARGUMENT_NAMES
	       . length;
	  i++ )
    {
	/* "_TDL_MARKUSED ( 'argument[i]' );" */
      theOutputDestination
	. write ( DataComponent.CXX_MARK_AS_USED_TO_COMPILER );
      theOutputDestination . write ( " ( " );
      theOutputDestination . write (
       DataComponent
	. CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_FUNCTION_ARGUMENT_NAMES [ i ] );
      theOutputDestination . write ( " );\n" );
    }
    theOutputDestination . write ( "\n" );


	/* If there are no arguments, there is no fancy struct. *
	 * Just a (void *)NULL value.                           */
    if ( getTaskArgumentCount() > 0 )
    {
	/* _TDL_DISTRIBUTED_STRUCT and _TDL_DISTRIBUTED_STRUCT_POINTER
	 * declaration
	 */
      generateCxxDistributedStruct ( theOutputDestination );


	/* _TDL_DISTRIBUTED_STRUCT_POINTER = (cast) argument */
      theOutputDestination
	. write ( DataComponent.CXX_DISTRIBUTED_STRUCT_POINTER );
      theOutputDestination . write ( " = (" );
      theOutputDestination
	. write ( DataComponent.CXX_DISTRIBUTED_STRUCT_NAME );
      theOutputDestination . write ( " *) " );
      theOutputDestination . write (
	    DataComponent.CXX_CREATE_DISTRIBUTED_REMOTE_ACTION_VOID_ARGUMENT );
      theOutputDestination . write ( ";\n" );

    } /* if ( getTaskArgumentCount() > 0 ) */


	/* return _TDL_CreateAction_foo ( ... ) */
    theOutputDestination . write ( "return " );
    theOutputDestination
      . write ( DataComponent.CXX_CREATEACTION_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	/* Lets indent all the arguments nicely... */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );
    
	/* First argument:  _TDL_ENCLOSING_TASK, */
    theOutputDestination . write ( DataComponent.CXX_ENCLOSING_TASK_REF );


	/* Second argument: _TDL_InvokeDelayedAllocation_*(), */
    theOutputDestination . write ( ",\n" );
	/* Note:  This next if pretty much ALWAYS has to be false. *
	 * But just to play it safe against future expansions...   */
    if ( isCxxDistributedOnlySubset ( theSubsetToProduce ) )
      theOutputDestination . write (
	DataComponent.CXX_INVOKE_AS_DISTRIBUTED_ONLY_DELAYED_ALLOCATION );
    else
      theOutputDestination . write (
	DataComponent
	 . CXX_INVOKE_AS_EITHER_LOCAL_OR_DISTRIBUTED_DELAYED_ALLOCATION );
    theOutputDestination . write ( "()" );


	/* Next arguments:  The actual, unpacked data.            *
	 * Conveniently, if there are no arguments, we skip this. */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
      theOutputDestination . write ( ",\n" );

	/* ptr -> arg, */
      theOutputDestination
	. write ( DataComponent.CXX_DISTRIBUTED_STRUCT_POINTER );
      theOutputDestination . write ( " -> " );
      
      theOutputDestination . setStripLeadingWhitespace();
      theOutputDestination
	. write ( getTaskArgument ( i ) . getArgumentName() );
    }

    theOutputDestination . write ( " );\n" );

	/* Lets stop indenting the arguments... */
    theOutputDestination . removeIndent ( tmpIndent );

	/* And end the body. */
    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
    theOutputDestination . write ( "}\n\n\n" );
    
  } /* void generateCxxCreateDistributedAction ( ... ) */




  protected void generateCxxDistributedStruct (
					 DataDestination theOutputDestination )
  {
	/* Idiocy check. */
    if ( getTaskArgumentCount() <= 0 )
    {
      System.err.println("[DataTaskDefinition:generateCxxDistributedStruct]  "
			 + "Internal Warning:  No arguments present.  "
			 + "This method should have never been called.  " );
      return;
    }

    theOutputDestination . write ( "struct " );
    theOutputDestination . write ( DataComponent.CXX_DISTRIBUTED_STRUCT_NAME );
    theOutputDestination . write ( "\n{" );
    theOutputDestination . addIndent (DataComponent.STANDARD_INDENT);

	/* Write Task Arguments */
    for ( int i=0;  i < getTaskArgumentCount();  i++ )
    {
	    /* Deal with #line macros */
      theOutputDestination . setUsingTdlFileName ( true );
      theOutputDestination
	. makeNextLineNumber ( getTaskArgument ( i ) . getLineNumber() );

      if ( i > 0 )
	theOutputDestination . write ( ";\n" );
      else
	theOutputDestination . write ( "\n" ); /* Necessary for #line macros */
      theOutputDestination . setStripLeadingWhitespace();

      getTaskArgument ( i )
	. generate ( theOutputDestination,
		     DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS );
    }

      	/* Deal with #line macros */
    theOutputDestination . setUsingTdlFileName ( false );

    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
    theOutputDestination . write ( ";\n} * " );
    theOutputDestination
      . write ( DataComponent.CXX_DISTRIBUTED_STRUCT_POINTER );
    theOutputDestination . write ( ";\n" );    

  } /* void generateCxxDistributedStruct ( ... ) */




	/* Internal routine to generateCxx */
  protected void generateCxxSpawnAndWait (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    int   i, taskArgumentSubset, ifIndent, argIndent;

    if(  ( getTaskScope() . hasScope() == false )
       &&(   ( theSubsetToProduce == DataComponent.CXX_HEADER                 )
	  || ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER            )
	  || ( getIsExtern()      == true                                     )
	  ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE )
	     || ( theSubsetToProduce
		              == DataComponent.CXX_HEADER_INLINED_FUNCTIONS ) )
    {
      theOutputDestination . write ( "inline " );
    }

    if ( getIsStatic() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.STATIC );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

    if ( getIsVirtual() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.VIRTUAL );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

    theOutputDestination . write ( DataComponent.CXX_SPAWNWAIT_RETURN_VALUE  );
    theOutputDestination . write ( "\n" );
	/* Only write scope outside of classes, in the code generation. */
    if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
      getTaskScope() . writeScope ( theOutputDestination );
    theOutputDestination . write ( DataComponent.CXX_SPAWNWAIT_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	/* Lets indent all the arguments nicely... */
    argIndent = theOutputDestination . indentToCurrentColumn ( );

	/* Write the first argument */
    theOutputDestination . write ( DataComponent.CXX_SPAWNWAIT_ARGUMENT_TYPE );
    theOutputDestination . write ( " " );
    theOutputDestination . write ( DataComponent.CXX_SPAWNWAIT_ARGUMENT_NAME );

	/* Write Task Arguments as spawnAndWait function arguments */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
	    /* Deal with #line macros */
      theOutputDestination . setUsingTdlFileName ( true );
      theOutputDestination
	. makeNextLineNumber ( getTaskArgument ( i ) . getLineNumber() );

      theOutputDestination . write ( ",\n" );
      theOutputDestination . setStripLeadingWhitespace();

	/* Comment out default argument unless we are in the header... */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == true )
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_EQUALS;
      else
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS;

      getTaskArgument ( i ). generate ( theOutputDestination,
					taskArgumentSubset   );
    }
      	/* Deal with #line macros */
    theOutputDestination . setUsingTdlFileName ( false );

    theOutputDestination . write ( " )" );

	/* Lets stop indenting the arguments... */
    theOutputDestination . removeIndent ( argIndent );


	/* Write the body... */
    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( " ;\n\n" );
    }
    else
    {
      theOutputDestination . write ( "\n{\n" );
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

      DataSpawnTask . generateCxxOutsideOfTask ( theOutputDestination,
						 theSubsetToProduce,
						 this,
						 ( needsUniqueIdString()
						  ? getUniqueIdString() : "" )
						);

      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( "}\n\n" );

    } /* IF ( writing spawn-and-wait body ) */

  } /* void generateCxxSpawnAndWait ( ... ) */




      /* Internal routine to generateCxx */
      /* Generate the external/inlined WAIT'ed-task-invocation as a function */
  protected void generateCxxFunctionalInvocation (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    int   i, taskArgumentSubset, argIndent;

    if(  ( getTaskScope() . hasScope() == false )
       &&(   ( theSubsetToProduce == DataComponent.CXX_HEADER                 )
	  || ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER            )
	  || ( getIsExtern()      == true                                     )
	  ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE )
	     || ( theSubsetToProduce
		              == DataComponent.CXX_HEADER_INLINED_FUNCTIONS ) )
    {
      theOutputDestination . write ( "inline " );
    }

    if ( getIsStatic() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.STATIC );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

    if ( getIsVirtual() )
    {
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "/*" );
      theOutputDestination . write ( DataTaskDefinition.VIRTUAL );
      if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
	theOutputDestination . write ( "*/" );
      theOutputDestination . write ( " " );
    }

    theOutputDestination . write ( DataComponent.CXX_SPAWNWAIT_RETURN_VALUE  );
    theOutputDestination . write ( "\n" );
	/* Only write scope outside of classes, in the code generation. */
    if ( /*NOT*/! isCxxHeaderSubset ( theSubsetToProduce ) )
      getTaskScope() . writeScope ( theOutputDestination );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	/* Lets indent all the arguments nicely... */
    argIndent = theOutputDestination . indentToCurrentColumn ( );

	/* Write Task Arguments as functional arguments */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
	    /* Deal with #line macros */
      theOutputDestination . setUsingTdlFileName ( true );
      theOutputDestination
	. makeNextLineNumber ( getTaskArgument ( i ) . getLineNumber() );

      if ( i > 0 )
	theOutputDestination . write ( ",\n" );

      else if ( theOutputDestination . getEnableLineMacros() )
	theOutputDestination . write ( "\n" );

      theOutputDestination . setStripLeadingWhitespace();

	/* Comment out default argument unless we are in the header... */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == true )
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_EQUALS;
      else
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS;

      getTaskArgument ( i ). generate( theOutputDestination,
				       taskArgumentSubset    );
    }
      	/* Deal with #line macros */
    theOutputDestination . setUsingTdlFileName ( false );

    theOutputDestination . write ( " )" );

	/* Lets stop indenting the arguments... */
    theOutputDestination . removeIndent ( argIndent );


	/* Write the body... */
    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( " ;\n\n" );
    }
    else
    {
      theOutputDestination . write ( "\n{\n" );
      theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

      theOutputDestination . write ( "return " );
      theOutputDestination
	. write ( DataComponent.CXX_SPAWNWAIT_FUNCTION_LEAD );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( " ( " );

	/* Lets indent all the arguments nicely... */
      argIndent = theOutputDestination . indentToCurrentColumn ( );

      theOutputDestination . write ( DataComponent.CXX_TCM_ROOT_NODE );

	/* Write the spawn-and-wait function-call arguments */
      for ( i=0;  i < getTaskArgumentCount();  i++ )
      {
	theOutputDestination . write ( ", " );
	theOutputDestination . write ( getTaskArgument ( i )
				         . getArgumentName() );
      }

      theOutputDestination . write ( " );\n" );

	/* Lets stop indenting the arguments... */
      theOutputDestination . removeIndent ( argIndent );

      theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );
      theOutputDestination . write ( "}\n\n" );

    } /* IF ( writing FunctionalInvocation body ) */

  } /* void generateCxxFunctionalInvocation ( ... ) */




	/* Internal routine to generateCxx */
  protected void generateCxxGetExceptionData (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    int i, tmpIndent;

    if ( getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
    {
      System.err.println( "[DataTaskDefinition:generateCxxGetExceptionData]"
			  + "  Warning:  Not an exception.  "
			  + "Aborting creating Exception-name method." );
      return;
    }

	/* Inlined code case -- It gets handled in the Header file... */
    if ( theSubsetToProduce == DataComponent.CXX_CODE_NO_FUNCTIONS )
      return;


    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
      theOutputDestination . write ( "virtual " );
    else
      theOutputDestination . write ( "/*virtual*/ " );


	/* Write "const void * <CLASS>::getExceptionData()" */
    theOutputDestination
      . write ( DataComponent.CXX_GET_EXCEPTION_DATA_RETURN_VALUE );

    if ( shouldDoCxxCodeFor ( theSubsetToProduce ) )
    {
      theOutputDestination . write ( "\n" );
      theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( "::" );
    }
    else
    {
      theOutputDestination . write ( " " );
    }

    theOutputDestination
      . write ( DataComponent.CXX_GET_EXCEPTION_DATA_METHOD );
    theOutputDestination . write ( "() const" );

    switch ( theSubsetToProduce )
    {
      case DataComponent.CXX_CODE:
      case DataComponent.INLINED_CXX_CODE:
      case DataComponent.CXX_HEADER_INLINED_FUNCTIONS:
	theOutputDestination . write ( "\n{\n" );

	   /* Indent body... */
	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	if ( getTaskArgumentCount() > 0 ) {
	    /* Write the struct version of the arguments */
	  theOutputDestination . write ( "static struct {" );
	    /* Indent the arguments... */
	  tmpIndent = theOutputDestination . indentToCurrentColumn ( );
	  theOutputDestination . write ( " " );
	    /* Write the components of the struct */
	  for ( i=0;  i < getTaskArgumentCount();  i++ ) {
	    getTaskArgument ( i )
		. generate ( theOutputDestination,
			     DataTaskArgument.TYPE_AND_NAME );
	    theOutputDestination . write ( ";\n" );
	  }
	  theOutputDestination . write ( "} " );
	  theOutputDestination
	      . write ( DataComponent.CXX_CREATOR_EXCEP_DATA_NAME );
	  theOutputDestination . write ( ";\n" );
	  theOutputDestination . removeIndent ( tmpIndent );

	    /* Set the components of the struct */
	  for ( i=0;  i < getTaskArgumentCount();  i++ ) {
	    theOutputDestination
		. write ( DataComponent.CXX_CREATOR_EXCEP_DATA_NAME );
	    theOutputDestination . write ( "." );
	    theOutputDestination 
		. write ( getTaskArgument ( i ) . getArgumentName() );
	    theOutputDestination . write ( " = " );
	    theOutputDestination 
		. write ( getTaskArgument ( i ) . getArgumentName() );
	    theOutputDestination . write ( ";\n" );
	  }

	    /* Return a pointer to the struct */
	  theOutputDestination . write ( "return (" );
	  theOutputDestination
	      . write ( DataComponent.CXX_GET_EXCEPTION_DATA_RETURN_VALUE );
	  theOutputDestination . write ( ") & " );
	  theOutputDestination
	      . write ( DataComponent.CXX_CREATOR_EXCEP_DATA_NAME );
	} else {
	  theOutputDestination . write ( "return (" );
	  theOutputDestination
	      . write ( DataComponent.CXX_GET_EXCEPTION_DATA_RETURN_VALUE );
	  theOutputDestination . write ( ") NULL" );
	}

	   /* Stop indenting the body. */
	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( ";\n}\n\n" );
	break;


      case DataComponent.CXX_HEADER:
      case DataComponent.CXX_BARE_HEADER:
      case DataComponent.CXX_CODE_AND_HEADER:
      default:
	theOutputDestination . write ( ";\n" );
	break;
    }

  } /* void generateCxxGetExceptionData ( ... ) */


  protected void generateCxxStaticExceptionName (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    if ( getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
    {
      System.err.println( "[DataTaskDefinition:generateCxxStaticExceptionName]"
			  + "  Warning:  Not an exception.  "
			  + "Aborting creating Exception-name method." );
      return;
    }

	/* Inlined code case -- It gets handled in the Header file... */
    if ( theSubsetToProduce == DataComponent.CXX_CODE_NO_FUNCTIONS )
      return;


    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
      theOutputDestination . write ( "static  " );
    else
      theOutputDestination . write ( "/*static*/ " );


	/* Write "const char * <CLASS>::getStaticExceptionName()" */
    theOutputDestination
      . write ( DataComponent.CXX_STATIC_EXCEPTION_NAME_RETURN_VALUE );

    if ( shouldDoCxxCodeFor ( theSubsetToProduce ) )
    {
      theOutputDestination . write ( "\n" );
      theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( "::" );
    }
    else
    {
      theOutputDestination . write ( " " );
    }

    theOutputDestination
      . write ( DataComponent.CXX_STATIC_EXCEPTION_NAME_METHOD );
    theOutputDestination . write ( "()" );


    switch ( theSubsetToProduce )
    {
      case DataComponent.CXX_CODE:
      case DataComponent.INLINED_CXX_CODE:
      case DataComponent.CXX_HEADER_INLINED_FUNCTIONS:
	theOutputDestination . write ( "\n{" );

	   /* Indent body... */
	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( "\nreturn \"" );
	theOutputDestination . write ( getTaskName() );

	   /* Stop indenting the body. */
	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( "\";\n}\n\n" );
	break;



      case DataComponent.CXX_HEADER:
      case DataComponent.CXX_BARE_HEADER:
      case DataComponent.CXX_CODE_AND_HEADER:
      default:
	theOutputDestination . write ( ";\n" );
	break;
    }

  } /* void generateCxxStaticExceptionName ( ... ) */



	/* Internal routine to generateCxx */
  protected void generateCxxExceptionName (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    if ( getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
    {
      System.err.println ( "[DataTaskDefinition:generateCxxExceptionName]  "
			   + "Warning:  Not an exception.  "
			   + "Aborting creating Exception-name method." );
      return;
    }


    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
      theOutputDestination . write ( "virtual " );
    else
      theOutputDestination . write ( "/*virtual*/ " );


	/* Write "const char * <CLASS>::getExceptionName() const" */
    theOutputDestination
      . write ( DataComponent.CXX_EXCEPTION_NAME_RETURN_VALUE );

    if ( shouldDoCxxCodeFor ( theSubsetToProduce ) )
    {
      theOutputDestination . write ( "\n" );
      theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( "::" );
    }
    else
    {
      theOutputDestination . write ( " " );
    }

    theOutputDestination
      . write ( DataComponent.CXX_EXCEPTION_NAME_METHOD );
    

    switch ( theSubsetToProduce )
    {
      case DataComponent.CXX_CODE:
      case DataComponent.INLINED_CXX_CODE:
      case DataComponent.CXX_CODE_NO_FUNCTIONS:
	theOutputDestination . write ( "\n{" );

	   /* Indent body... */
	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( "\nreturn " );

	   /* Stop indenting the body. */
	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( "::" );
	theOutputDestination
	  . write ( DataComponent.CXX_STATIC_EXCEPTION_NAME_METHOD );
	theOutputDestination . write ( "();\n}\n\n" );
	break;



      case DataComponent.CXX_HEADER:
      case DataComponent.CXX_HEADER_INLINED_FUNCTIONS:
      case DataComponent.CXX_BARE_HEADER:
      case DataComponent.CXX_CODE_AND_HEADER:
      default:
	theOutputDestination . write ( ";\n" );
	break;
    }

  } /* void generateCxxExceptionName ( ... ) */



	/* Internal routine to generateCxx */
  protected void generateCxxExceptionMatchesString (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    int  tmpIndent;

    if ( getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
    {
      System.err.println (
		      "[DataTaskDefinition:generateCxxExceptionMatchesStrng]  "
		      + "Warning:  Not an exception.  "
		      + "Aborting creating Exception-matches method." );
      return;
    }

	/* Inlined code case -- It gets handled in the Header file... */
    if ( theSubsetToProduce == DataComponent.CXX_CODE_NO_FUNCTIONS )
      return;


	/* Space us down a line for inlined code */
    if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE            )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER_INLINED_FUNCTIONS))
    {
      theOutputDestination . write ( "\n" );
    }


    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
      theOutputDestination . write ( "virtual " );
    else
      theOutputDestination . write ( "/*virtual*/ " );


	/* Write:
	 *   BOOLEAN <CLASS>::matches ( const char * theString ) const
	 */
    theOutputDestination
      . write ( DataComponent.CXX_EXCEPTION_MATCHES_RETURN_VALUE );

    if ( shouldDoCxxCodeFor ( theSubsetToProduce ) )
    {
      theOutputDestination . write ( "\n" );
      theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( "::" );
    }
    else
    {
      theOutputDestination . write ( " " );
    }

    theOutputDestination
      . write( DataComponent.CXX_EXCEPTION_MATCHES_METHOD );

    
    switch ( theSubsetToProduce )
    {
      case DataComponent.CXX_CODE:
      case DataComponent.INLINED_CXX_CODE:
      case DataComponent.CXX_HEADER_INLINED_FUNCTIONS:
	theOutputDestination . write ( "\n{" ); 

	   /* Indent body... */
	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( "\nreturn ( " );
	theOutputDestination . write ( DataComponent.CXX_TDL_STRING_EQUAL );
	theOutputDestination . write ( " ( " );

	  /* Need to do more indenting here... */
	tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( "::" );
	theOutputDestination
	  . write ( DataComponent.CXX_STATIC_EXCEPTION_NAME_METHOD );
	theOutputDestination . write ( "(),\n" );

	theOutputDestination
	  . write ( DataComponent.CXX_EXCEPTION_MATCHES_ARGUMENT );
	theOutputDestination . write ( " ) == TRUE )" );

	  /* That's enough indenting... */
	theOutputDestination . removeIndent ( tmpIndent );

	theOutputDestination . write ( "\n    || " );
	if ( getExceptionBaseTask() != null )
	{
	  theOutputDestination
	    . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
	  theOutputDestination
	    . write ( getExceptionBaseTask() . getTaskName() );
	}
	else
	{
	  theOutputDestination
	    . write ( DataComponent.CXX_EXCEPTION_BASE_CLASS );
	}
	theOutputDestination . write ( "::" );	
	theOutputDestination
	  . write ( DataComponent.CXX_EXCEPTION_MATCHES_METHOD_NAME );
	theOutputDestination . write ( " ( " );	
	theOutputDestination
	  . write ( DataComponent.CXX_EXCEPTION_MATCHES_ARGUMENT );

	   /* Stop indenting the body. */
	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( " );\n}\n\n\n" );
	break;



      case DataComponent.CXX_HEADER:
      case DataComponent.CXX_BARE_HEADER:
      case DataComponent.CXX_CODE_AND_HEADER:
      default:
	theOutputDestination . write ( ";\n" );
	break;
    }

  } /* void generateCxxExceptionMatchesString ( ... ) */

  protected void generateCxxExceptionCloneString (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    int  tmpIndent;

    if ( getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
    {
      System.err.println (
		      "[DataTaskDefinition:generateCxxExceptionMatchesStrng]  "
		      + "Warning:  Not an exception.  "
		      + "Aborting creating Exception-matches method." );
      return;
    }

	/* Inlined code case -- It gets handled in the Header file... */
    if ( theSubsetToProduce == DataComponent.CXX_CODE_NO_FUNCTIONS )
      return;


	/* Space us down a line for inlined code */
    if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE            )
	|| ( theSubsetToProduce == DataComponent.CXX_HEADER_INLINED_FUNCTIONS))
    {
      theOutputDestination . write ( "\n" );
    }


    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
      theOutputDestination . write ( "virtual " );
    else
      theOutputDestination . write ( "/*virtual*/ " );


	/* Write:
	 *   TCM_Exception *<CLASS>::clone ( void ) const
	 */
    theOutputDestination
      . write ( DataComponent.CXX_EXCEPTION_CLONE_RETURN_VALUE );

    if ( shouldDoCxxCodeFor ( theSubsetToProduce ) )
    {
      theOutputDestination . write ( "\n" );
      theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
      theOutputDestination . write ( getTaskName() );
      theOutputDestination . write ( "::" );
    }
    else
    {
      theOutputDestination . write ( " " );
    }

    theOutputDestination . write( DataComponent.CXX_EXCEPTION_CLONE_METHOD );

    switch ( theSubsetToProduce )
    {
      case DataComponent.CXX_CODE:
      case DataComponent.INLINED_CXX_CODE:
      case DataComponent.CXX_HEADER_INLINED_FUNCTIONS:
	theOutputDestination . write ( "\n{" ); 

	   /* Indent body... */
	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( "\nreturn " );
	theOutputDestination
	    . write ( DataComponent.CXX_CREATE_EXCEPTION_FUNCTION_LEAD );
	theOutputDestination . write ( getTaskName() );
	theOutputDestination . write ( " ( " );

	  /* Indent the arguments... */
	tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	/* Write the task-argument-names as arguments to the new... */
	for ( int i=0;  i < getTaskArgumentCount();  i++ ) {
	  if ( i > 0 )
	    theOutputDestination . write ( ",\n" );
	  theOutputDestination . setStripLeadingWhitespace();
	  theOutputDestination
	      . write ( getTaskArgument ( i ) . getArgumentName() );
	}

	/* Lets stop indenting the new arguments... */
	theOutputDestination . removeIndent ( tmpIndent );

	theOutputDestination . write ( " );" );
	   /* Stop indenting the body. */
	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( "\n}\n\n\n" );
	break;

      case DataComponent.CXX_HEADER:
      case DataComponent.CXX_BARE_HEADER:
      case DataComponent.CXX_CODE_AND_HEADER:
      default:
	theOutputDestination . write ( ";\n" );
	break;
    }

  } /* void generateCxxExceptionCloneString ( ... ) */


	/* Internal routine to generateCxx */
  protected void generateCxxCreateException (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    int i, taskArgumentSubset, tmpIndent;

    if ( getTaskType() != DataTaskDefinition.EXCEPTION_TASK )
    {
      System.err.println ( "[DataTaskDefinition:generateCxxCreateException]  "
			   + "Warning:  Not an exception.  "
			   + "Aborting creating create-Exception function." );
      return;
    }

    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE )
	     || ( theSubsetToProduce
		              == DataComponent.CXX_HEADER_INLINED_FUNCTIONS ) )
    {
      theOutputDestination . write ( "inline " );
    }

    theOutputDestination
      . write ( DataComponent.CXX_CREATE_EXCEPTION_RETURN_VALUE  );
    theOutputDestination . write ( "\n" );
    theOutputDestination
      . write ( DataComponent.CXX_CREATE_EXCEPTION_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	/* Lets indent all the arguments nicely... */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	/* Write Task Arguments */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
	    /* Deal with #line macros */
      theOutputDestination . setUsingTdlFileName ( true );
      theOutputDestination
	. makeNextLineNumber ( getTaskArgument ( i ) . getLineNumber() );

      if ( i > 0 )
	theOutputDestination . write ( ",\n" );
      else if ( theOutputDestination . getEnableLineMacros() )
	theOutputDestination . write ( "\n" );
      theOutputDestination . setStripLeadingWhitespace();

	/* Comment out default argument unless we are in the header... */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == true )
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_EQUALS;
      else
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS;

      getTaskArgument ( i ) . generate ( theOutputDestination,
					 taskArgumentSubset   );
    }
      	/* Deal with #line macros */
    theOutputDestination . setUsingTdlFileName ( false );

    theOutputDestination . write( " )" );

	/* Lets stop indenting the arguments... */
    theOutputDestination . removeIndent ( tmpIndent );


	/* Are we NOT writing the body??? */
    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( " ;\n\n" );
      return;
    }

	/* Otherwise, write the body... */

    theOutputDestination . write ( "\n{\n" );
    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
    theOutputDestination .
	write ( DataComponent.CXX_CREATE_EXCEPTION_RETURN_VALUE );
    theOutputDestination . write ( " exception = new " );

    theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	  /* Indent the arguments... */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	  /* Write the task-argument-names as arguments to the new... */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
      if ( i > 0 )
	theOutputDestination . write ( ",\n" );
      theOutputDestination . setStripLeadingWhitespace();
      theOutputDestination
	. write ( getTaskArgument ( i ) . getArgumentName() );
    }

	/* Lets stop indenting the new arguments... */
    theOutputDestination . removeIndent ( tmpIndent );

    theOutputDestination . write ( " );\n" );

    theOutputDestination . write ( "exception->setExceptionName( (char *)\"" );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( "\" );\n" );

    theOutputDestination . write ( "return exception;" );

    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

    theOutputDestination . write ( "\n}\n\n\n" );

  } /* void generateCxxCreateException ( ... ) */




	/* Internal routine to generateCxx */
  protected void generateCxxHandledExceptionName (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
    /* throws CompilationException */
  {
    if ( getTaskType() != DataTaskDefinition.HANDLER_TASK )
    {
      System.err.println (
		       "[DataTaskDefinition:generateCxxHandledExceptionName]  "
		       + "Warning:  Not an exception handler.  "
		       + "Aborting creating Exception-Handler-name method." );
      return;
    }

    if (   ( getHandlesException()            == null )
	|| ( getHandlesException() . length() <= 0    ) )
    {
      throw new CompilationException ( "Internal Error:  DataTaskDefinition "
				       + "object of type \"EXCEPTION HANDLER\""
				       + " does not handle an exception." );
    }

    if ( shouldDoCxxHeaderClassFor ( theSubsetToProduce ) )
      theOutputDestination . write ( "virtual " );
    else
      theOutputDestination . write ( "/*virtual*/ " );


	/* Write: const char * <CLASS>::_TDL_getHandledExceptionName() const */
    theOutputDestination
      . write ( DataComponent.CXX_HANDLED_EXCEPTION_NAME_RETURN_VALUE );

    if ( shouldDoCxxCodeFor ( theSubsetToProduce ) )
    {
      theOutputDestination . write ( "\n" );
      theOutputDestination . write ( DataComponent.CXX_HANDLER_NAME_LEAD );
      theOutputDestination . write ( getTaskName() );
      writeUniqueIdStringIfNeeded  ( theOutputDestination );
      theOutputDestination . write ( "::" );
    }
    else
    {
      theOutputDestination . write ( " " );
    }

    theOutputDestination
      . write ( DataComponent.CXX_HANDLED_EXCEPTION_NAME_METHOD );
    

    switch ( theSubsetToProduce )
    {
      case DataComponent.CXX_CODE:
      case DataComponent.INLINED_CXX_CODE:
      case DataComponent.CXX_CODE_NO_FUNCTIONS:
	theOutputDestination . write ( "\n{" );

	   /* Indent body... */
	theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( "\nreturn " );

	   /* Stop indenting the body. */
	theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

	theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
	theOutputDestination . write ( getHandlesException() );
	theOutputDestination . write ( "::" );
	theOutputDestination
	  . write ( DataComponent.CXX_STATIC_EXCEPTION_NAME_METHOD );
	theOutputDestination . write ( "();\n}\n\n" );
	break;



      case DataComponent.CXX_HEADER:
      case DataComponent.CXX_HEADER_INLINED_FUNCTIONS:
      case DataComponent.CXX_BARE_HEADER:
      case DataComponent.CXX_CODE_AND_HEADER:
      default:
	theOutputDestination . write ( ";\n" );
	break;
    }

  } /* void generateCxxHandledExceptionName ( ... ) */





	/* Internal routine to generateCxx */
  protected void generateCxxCreateExceptionHandler (
					DataDestination  theOutputDestination,
					int              theSubsetToProduce   )
  {
    DataConstraint  dataConstraint;
    int             i, taskArgumentSubset, tmpIndent;


    if ( getTaskType() != DataTaskDefinition.HANDLER_TASK )
    {
      System.err.println (
		     "[DataTaskDefinition:generateCxxCreateExceptionHandler]  "
		     + "Warning:  Not an exception handler.  "
		     + "Aborting creating CreateExceptionHandler function." );
      return;
    }

    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( "extern " );
    }
    else if (   ( theSubsetToProduce == DataComponent.INLINED_CXX_CODE )
	     || ( theSubsetToProduce
		              == DataComponent.CXX_HEADER_INLINED_FUNCTIONS ) )
    {
      theOutputDestination . write ( "inline " );
    }

    theOutputDestination
      . write ( DataComponent.CXX_CREATE_HANDLER_RETURN_VALUE  );
    theOutputDestination . write ( "\n" );
    theOutputDestination
      . write ( DataComponent.CXX_CREATE_HANDLER_FUNCTION_LEAD );
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( " ( " );

	/* Lets indent all the arguments nicely... */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	/* Write Task Arguments */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
	    /* Deal with #line macros */
      theOutputDestination . setUsingTdlFileName ( true );
      theOutputDestination
	. makeNextLineNumber ( getTaskArgument ( i ) . getLineNumber() );

      if ( i > 0 )
	theOutputDestination . write ( ",\n" );
      else if ( theOutputDestination . getEnableLineMacros() )
	theOutputDestination . write ( "\n" );

      theOutputDestination . setStripLeadingWhitespace();

	/* Comment out default argument unless we are in the header... */
      if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == true )
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_EQUALS;
      else
	taskArgumentSubset = DataTaskArgument.TYPE_NAME_AND_COMMENTED_EQUALS;

      getTaskArgument ( i ) . generate ( theOutputDestination,
					 taskArgumentSubset   );
    }
      	/* Deal with #line macros */
    theOutputDestination . setUsingTdlFileName ( false );


	/* Write optional maximum-activation argument... */
    if ( getTaskArgumentCount() > 0 )
      theOutputDestination . write ( ",\n" );      

    theOutputDestination
      . write ( DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_ARGUMENT_TYPE );
    theOutputDestination . write ( " " );

    theOutputDestination
      . write ( DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_ARGUMENT );

	/* Comment out default argument unless we are in the header... */
    if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
      theOutputDestination . write ( " /*" );

    theOutputDestination . write ( " = 0 " );

	/* Don't use CXX_CONSTANT_MAXIMUM_ACTIVATIONS_DEFAULT_VALUE here.
	 * We need a value of "0" for the ?: operation down below.
	 */

    if ( shouldDoDefaultArgumentsFor ( theSubsetToProduce ) == false )
      theOutputDestination . write ( "*/" );

    theOutputDestination . write ( ")" );

	/* Lets stop indenting the arguments... */
    theOutputDestination . removeIndent ( tmpIndent );



	/* Are we NOT writing the body??? */
    if (   ( theSubsetToProduce == DataComponent.CXX_HEADER      )
	|| ( theSubsetToProduce == DataComponent.CXX_BARE_HEADER )
	|| ( getIsExtern()      == true                          ) )
    {
      theOutputDestination . write ( " ;\n\n" );
      return;
    }

	/* Otherwise, write the body... */

    theOutputDestination . write ( "\n{\n" );
    theOutputDestination . addIndent ( DataComponent.STANDARD_INDENT );
    theOutputDestination . write ( "return new " );
    theOutputDestination . write ( DataComponent.CXX_HANDLER_NAME_LEAD );
    theOutputDestination . write ( getTaskName() );
    writeUniqueIdStringIfNeeded  ( theOutputDestination );
    theOutputDestination . write ( " ( " );

	  /* Indent the arguments... */
    tmpIndent = theOutputDestination . indentToCurrentColumn ( );

	  /* Write the task-argument-names as arguments to the new... */
    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
      if ( i > 0 )
	theOutputDestination . write ( ",\n" );
      theOutputDestination . setStripLeadingWhitespace();
      theOutputDestination
	. write ( getTaskArgument ( i ) . getArgumentName() );
    }


	/* Add Maximim Activations to Exception Handlers... */
    if ( getTaskArgumentCount() > 0 )
      theOutputDestination . write ( ",\n" );

    theOutputDestination . write ( "  ( " );
    theOutputDestination
      . write ( DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_ARGUMENT );
    theOutputDestination . write ( " != 0 )\n?   " );
    theOutputDestination
      . write ( DataComponent.CXX_CONSTANT_MAXIMUM_ACTIVATIONS_ARGUMENT );
    
    dataConstraint = findConstraint ( DataConstraint.MAXIMUM_ACTIVATE );

    if (   ( dataConstraint != null )
	&& ( theOutputDestination . getEnableLineMacros() == true ) )
    {
      theOutputDestination . setUsingTdlFileName ( true );
      theOutputDestination
	. makeNextLineNumber  ( dataConstraint . getNumericExpression()
					       . getLineNumber() );
    }

    theOutputDestination . write ( "\n: ( " );

    if ( dataConstraint != null )
    {
	  /* Indent this too... */
      tmpIndent += theOutputDestination . indentToCurrentColumn ( );

      theOutputDestination . setStripLeadingWhitespace();
      theOutputDestination . setPersistentlyStripLeadingSpaces ( true );

      dataConstraint . getNumericExpression()
		     . generate ( theOutputDestination,
				  DataComponent.ENTIRE_OBJECT );

      theOutputDestination . setPersistentlyStripLeadingSpaces ( false );

      if ( theOutputDestination . getEnableLineMacros() == true )
      {
	theOutputDestination . setUsingTdlFileName ( false );
	theOutputDestination . write ( "\n" ); /* Flush #line macro */
      }
    }
    else
    {
      theOutputDestination
	. write ( DataComponent.CXX_TCM_DEFAULT_MAX_ACTIVATES );
    }

    theOutputDestination . write ( " )" );




	/* Lets stop indenting the new arguments... */
    theOutputDestination . removeIndent ( tmpIndent );

    theOutputDestination . removeIndent ( DataComponent.STANDARD_INDENT );

    theOutputDestination . write ( " );\n}\n\n\n" );

  } /* void generateCxxCreateExceptionHandler ( ... ) */




	/* We need to re-declare the arguments to prevent them from being
	 * modified when this task is invoked multiple times...
	 * Note:  Invoked through use of DataComponentPlaceholder().
	 */
  public void generateArgumentsDeclarationCode (
				     boolean          theMarkDeclarationsUsed,
				     DataDestination  theOutputDestination    )
  {
    int  i;

	   /* Deal with #line macros. */
    if ( getTaskArgumentCount() > 0 )
      theOutputDestination . setUsingTdlFileName ( true );

    for ( i=0;  i < getTaskArgumentCount();  i++ )
    {
      theOutputDestination
	. makeNextLineNumber ( getTaskArgument ( i ) . getLineNumber() );
      theOutputDestination . write ( "\n" );

      theOutputDestination . setStripLeadingWhitespace();
      getTaskArgument ( i ) . generate ( theOutputDestination,
					 DataTaskArgument.TYPE_AND_NAME );
      theOutputDestination . write ( " = this -> " );
      theOutputDestination
	. write ( getTaskArgument ( i ) . getArgumentName() );

      if ( theMarkDeclarationsUsed == true )
      {
	theOutputDestination . write ( ";   " );
	theOutputDestination
	  . write ( DataComponent.CXX_MARK_AS_USED_TO_COMPILER );
	theOutputDestination . write ( " ( " );
	theOutputDestination
	  . write ( getTaskArgument ( i ) . getArgumentName() );
	theOutputDestination . write ( " )" );
      }

      theOutputDestination . write ( ";" );
    }

	   /* Deal with #line macros. */
    if (   ( getTaskArgumentCount() >  0            )
	|| ( getTaskType()          == HANDLER_TASK ) )
    {
	/* Note:  This can generate a #line\n#line situation.  Better two
	 * #line macros than using line 2 of the Task where it doesn't belong.
	 * Ie:  Goal foo( int i )  -- line 1
	 *      {                  -- line 2
         *         i++             -- line 3
	 * We don't want line 2 being used automatically (and incorrectly)
	 * in the generated output.  Therefore, we create:
	 *      #line ... cxxFile
	 *      #line ... tdlFile
	 * Right here to correct this problem.
	 *
	 * PS: HANDLER_TASK needs cxxFile and a leading newline too.
	 */
      theOutputDestination . setUsingTdlFileName ( false );
      theOutputDestination . write ( "\n" );
    }


    if ( getTaskType() == HANDLER_TASK )
    {
      theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
      theOutputDestination . write ( getHandlesException() );
      theOutputDestination . write ( " & " );
      theOutputDestination . write ( getHandlesException() );

      theOutputDestination . addIndent ( 2 * DataComponent.STANDARD_INDENT );

      theOutputDestination . write ( "\n= * ( (" );
      theOutputDestination . write ( DataComponent.CXX_EXCEPTION_NAME_LEAD );
      theOutputDestination . write ( getHandlesException() );
      theOutputDestination . write ( " *)\n      " );
      theOutputDestination
	. write ( DataComponent.CXX_HANDLER_EXCEPTION_DATA_ACCESS );
      theOutputDestination . write ( " );\n" );

      theOutputDestination . removeIndent( 2 * DataComponent.STANDARD_INDENT );

      theOutputDestination
	. write ( DataComponent.CXX_MARK_AS_USED_TO_COMPILER );
      theOutputDestination . write ( " ( " );
      theOutputDestination . write ( getHandlesException() );
      theOutputDestination . write ( " );\n" );
    }
  }




  public void generateHandleManagerDeclaration (
					 DataDestination theOutputDestination,
					 DataVector      theNonUniqueNames,
					 DataVector      theStatements,
					 DataHashtable   theOnAgentHashtable )
  {
    DataStatement       dataStatement;
    DataSpawnTask       dataSpawnTask = null;
    DataLabelStatement  dataLabelStatement;
    DataComponent       dataComponent;
    int                 i, nameIndent, nameCount, nameConstraintIndex;

	/* Idiocy checks */
    if ( theNonUniqueNames == null )
    {
      System.err.println (
		       "[DataTaskDefinition:generateHandleManagerDeclaration] "
		       + "Error:  theNonUniqueNames is null.  "
		       + "Unable to generate HandleManager declaration." );
      return;
    }
    if ( theStatements == null )
    {
      System.err.println (
		       "[DataTaskDefinition:generateHandleManagerDeclaration] "
		       + "Warning: theStatements is null.  Regenerating..." );
      theStatements = generateStatementsVector();
    }
    if ( theOnAgentHashtable == null )
    {
      System.err.println (
		       "[DataTaskDefinition:generateHandleManagerDeclaration] "
		       + "Error:  theOnAgentHashtable is null.  "
		       + "Unable to generate HandleManager declaration." );
      return;
    }


	/* Abort if there are no spawns or with-do statements */
    for ( i = 0;   i < theStatements.count();   i ++ )
    {
      if ( ( (DataStatement)  (theStatements . elementAt ( i )) )
	     . isSpawnRelatedStatement() == true )
      {
	break;
      }
    }
    if ( i >= theStatements.count() )
    {
      return;
    }


	/* Write initial declaration */
    theOutputDestination . write ( "\n" );
    theOutputDestination . write ( DataComponent.CXX_TDL_TASK_HANDLER_CLASS );
    theOutputDestination . write ( "   " );
    theOutputDestination
      . write ( DataComponent.CXX_TDL_TASK_HANDLER_INSTANCE );
    theOutputDestination . write ( " ( \"" );
    getTaskScope() . writeScope  ( theOutputDestination );    
    theOutputDestination . write ( getTaskName() );
    theOutputDestination . write ( "\", " );
    theOutputDestination . write ( DataComponent.CXX_ENCLOSING_TASK_REF );
    theOutputDestination . write ( " );\n" );

	/* Write spawn/with-do name definitions */
    for ( i = 0;   i < theStatements.count();   i ++ )
    {
      if (   ( theStatements . elementAt ( i ) instanceof DataSpawnTask      )
	  || ( theStatements . elementAt ( i ) instanceof DataWithDoStatement))
      {
	dataStatement = (DataStatement) ( theStatements . elementAt ( i ) );

	   /* Compute the number of names */

	nameCount = 1; /* Default identifier */

	   /* Is it a spawn-task & is the TaskName unique? */
	if (   ( dataStatement instanceof DataSpawnTask )
	    && ( theNonUniqueNames . contains (
		   ((DataSpawnTask) dataStatement) . getTaskName() ) == false )
	    )
	{
	  nameCount ++;
	}

	   /* Are there unique labels? */
	for ( dataLabelStatement  = dataStatement . getLabel();
	      dataLabelStatement != null;
	      dataLabelStatement  = dataLabelStatement . getLabel() )
	{
		/* Do we Have a label (non case/default) that does not
		 * match the task-name & is unique?
		 */
	  if (   ( dataLabelStatement . hasId() )
	      && (   ( ( dataStatement instanceof DataSpawnTask )    == false )
		  || ( ((DataSpawnTask) dataStatement) . getTaskName()
		           . equals ( dataLabelStatement . getId() ) == false )
		  )
	      && ( theNonUniqueNames . contains ( dataLabelStatement.getId() )
		   == false )
	      )
	  {
	    nameCount++;
	  }
	}


	    /* Write that puppy out */

	theOutputDestination
	  . write ( DataComponent.CXX_TDL_TASK_HANDLER_INSTANCE );

	   /* Indent remaining methods to this point */
	nameIndent = theOutputDestination . indentToCurrentColumn();


	   /* Write addEntry / addBranch */

	theOutputDestination . write ( " .  " );

	if ( dataStatement instanceof DataSpawnTask )
	{
		/* Big Question:  Is this a Distributed-only spawn, *
		 * a LOCAL-only spawn, or either?                   */
	  dataSpawnTask = ((DataSpawnTask) dataStatement);

	  if ( dataSpawnTask . hasTaskExpression() )
	  {
	    theOutputDestination . write (
	      DataComponent
	       . CXX_TDL_TASK_HANDLER_ADD_SPAWN_DELAYED_ALLOCATION_METHOD );
	    
	  }

	  else 
	  {
	    switch ( getDataSpawnTaskLocalOrDistributed ( dataSpawnTask,
							  theOnAgentHashtable,
							  theStatements ) )
	    {
	      case DataComponent.DISTRIBUTED_ONLY:
		theOutputDestination . write (
		  DataComponent
		   . CXX_TDL_TASK_HANDLER_ADD_SPAWN_DISTRIBUTED_METHOD
		);
if (TDLC.DEBUG_ENABLED == true )
 System.err.println("da0g-theOnAgentHashtable-true-distributed-only");
		break;
 
	      case DataComponent.EITHER_LOCAL_OR_DISTRIBUTED:
		theOutputDestination . write (
		  DataComponent.CXX_TDL_TASK_HANDLER_ADD_SPAWN_EITHER_METHOD );
if (TDLC.DEBUG_ENABLED == true )
 System.err.println("da0g-theOnAgentHashtable-true-distributed-either");
		break;

	      case DataComponent.LOCAL_NONDISTRIBUTED_ONLY:
		theOutputDestination . write (
		  DataComponent.CXX_TDL_TASK_HANDLER_ADD_SPAWN_LOCAL_METHOD );
if (TDLC.DEBUG_ENABLED == true )
 System.err.println("da0g-theOnAgentHashtable-false-local");
		break;

	      default:
		System.err.println (
		  "[DataTaskDefinition:generateHandleManagerDeclaration]  "
		  + "Internal Error:  getDataSpawnTaskLocalOrDistributed() "
		  + "returned an invalid value.  "
		  + "Assuming local non-distributed case.\n" );
		theOutputDestination . write (
		  DataComponent.CXX_TDL_TASK_HANDLER_ADD_SPAWN_LOCAL_METHOD );
		break;
	    } /* switch ( getDataSpawnTaskLocalOrDistributed (...) ) */
	  } /* if ( dataSpawnTask . hasTaskExpression() ) ... ELSE ... */
	} /* if ( dataStatement instanceof DataSpawnTask ) */
	else
	{
	  dataSpawnTask = null;
	  theOutputDestination
	    . write ( DataComponent.CXX_TDL_TASK_HANDLER_ADD_WITH_METHOD );
	}

	theOutputDestination . write ( " ( " );

	if (   ( dataStatement instanceof DataSpawnTask       )
	    && ( dataSpawnTask . hasTaskExpression() == false ) )
	{
	  dataSpawnTask
	    . getTaskScope() . writeScope ( theOutputDestination );
	  theOutputDestination
	    . write ( DataComponent.CXX_ALLOCATE_FUNCTION_LEAD );
	  theOutputDestination
	    . write ( dataSpawnTask . getTaskName() );
	  theOutputDestination . write ( ", " );
	}

	theOutputDestination . write ( "" + nameCount );
	theOutputDestination . write ( " )" );


	    /* If we have a SPAWN Statement with a NAME constraint...       */
	    /* (NAME constraints can only be applied at a SPAWN statement.) */
	if ( dataStatement instanceof DataSpawnTask )
	{
	  nameConstraintIndex
	    = DataConstraint.getLastIndexOfConstraintOfType (
					  DataConstraint.TCM_TASK_TREE_NAME,
					  (DataSpawnTask) dataStatement      );
	  if ( nameConstraintIndex >= 0 )
	  {
	    theOutputDestination . write ( "\n -> " );
	    theOutputDestination . write (
		     DataComponent .
		     CXX_TDL_TASK_HANDLER_SET_TCM_TASK_TREE_NODE_NAME_METHOD );
	    theOutputDestination . write ( " ( " );

		/* Deal with #line macros */
	    theOutputDestination . setUsingTdlFileName ( true );
	    theOutputDestination . makeNextLineNumber (
	      ((DataSpawnTask) dataStatement)
	        . getConstraint ( nameConstraintIndex )
	        . getTcmTaskTreeNameExpression() );

	    if ( theOutputDestination . getEnableLineMacros() )
	    {
	      theOutputDestination . write ( "\n" );
	    }

	    ((DataSpawnTask) dataStatement)
	      . getConstraint ( nameConstraintIndex )
	      . getTcmTaskTreeNameExpression()
	      . generate ( theOutputDestination,
			   DataComponent.ENTIRE_OBJECT );

		/* Deal with #line macros */
	    theOutputDestination . setUsingTdlFileName ( false );

	    theOutputDestination . write ( " )" );
	  }
	}


	  /* Write first addName -- Default Identifier */
	theOutputDestination . write ( "\n -> " );
	theOutputDestination
	  . write ( DataComponent.CXX_TDL_TASK_HANDLER_ADD_NAME_METHOD );
	theOutputDestination . write ( " ( \"" );
	if ( dataStatement instanceof DataSpawnTask )
	  theOutputDestination . write ( getIdentifierForSubtask (
					     (DataSpawnTask) dataStatement ) );
	else
	  theOutputDestination . write ( getIdentifierForBranch (
				       (DataWithDoStatement) dataStatement ) );
	theOutputDestination . write ( "\" )" );

	   /* Write addName -- Is the TaskName unique? */
	if (   ( dataStatement instanceof DataSpawnTask )
	    && ( theNonUniqueNames . contains (
		   ((DataSpawnTask) dataStatement) . getTaskName() ) == false )
	    )
	{
	  theOutputDestination . write ( "\n -> " );
	  theOutputDestination
	    . write ( DataComponent.CXX_TDL_TASK_HANDLER_ADD_NAME_METHOD );
	  theOutputDestination . write ( " ( \"" );
	  theOutputDestination
	    . write ( ((DataSpawnTask) dataStatement) . getTaskName() );
	  theOutputDestination . write ( "\" )" );
	}

	   /* Write addName -- Are there any unique labels? */
	for ( dataLabelStatement  = dataStatement . getLabel();
	      dataLabelStatement != null;
	      dataLabelStatement  = dataLabelStatement . getLabel() )
	{
		/* Do we Have a label (non case/default) that does not
		 * match the task-name & is unique?
		 */
	  if (   ( dataLabelStatement . hasId() )
	      && (   ( ( dataStatement instanceof DataSpawnTask )    == false )
		  || ( ((DataSpawnTask) dataStatement) . getTaskName()
		           . equals ( dataLabelStatement . getId() ) == false )
		  )
	      && ( theNonUniqueNames . contains ( dataLabelStatement.getId() )
		   == false )
	      )
	  {
	    theOutputDestination . write ( "\n -> " );
	    theOutputDestination
	      . write ( DataComponent.CXX_TDL_TASK_HANDLER_ADD_NAME_METHOD );
	    theOutputDestination . write ( " ( \"" );
	    theOutputDestination . write ( dataLabelStatement . getId() );
	    theOutputDestination . write ( "\" )" );
	  }
	}


	   /* If a Spawn'ed task is allocate()'ed (c++-library-side)
	    * as part of a constraint statement prior to that task being
	    * inserted into the with-tree hierarchy (c++-library-side)...
	    * Well we need some mechanism to determine what, if any, on-agent
	    * constraints should be applied...  E.g.: This case:
	    *  > w0: WITH ( on AGENT1)
	    *  > {
	    *  >   if ( booleanFunction() )
	    *  >     foo ON AGENT2;
	    *  >   w1: WITH ( on AGENT3 )
	    *  >   {
	    *  >     otherTask DISABLE HANDLING FOR 1.0 AFTER foo COMPLETED;
	    *  >   }
	    *  >   spawn foo();
	    *  > }
	    *  > spawn otherTask();
	    * The only way to solve the problem is to either outlaw this case,
	    * or to previously bind foo to the first <w0> with statement when
	    * we set up the _TDL_HandleManager _TDL_SpawnedTasks object.
	    *
	    * In the interests of future expansion and flexibility,
	    * we implement the latter here for both SPAWN and WITH cases.
	    */
	for ( dataComponent  = dataStatement . getParent();
	      dataComponent != null;
	      dataComponent  = dataComponent . getParent()
	     )
	{
	  if ( dataComponent instanceof DataWithDoStatement )
	  {
	    theOutputDestination . write ( "\n -> " );
	    theOutputDestination . write (
		  DataComponent.CXX_TDL_TASK_HANDLER_SET_DEFAULT_WITH_PARENT );
	    theOutputDestination . write ( " ( \"" );
	    theOutputDestination . write ( getIdentifierForBranch (
				       (DataWithDoStatement) dataComponent ) );
	    theOutputDestination . write ( "\" )" );
	    break; /* We are done.  EXIT FOR LOOP!!! */
	  } /* if ( dataComponent instanceof DataWithDoStatement ) */
	} /* FOR ( datacomponent = dataStatement.getParent ... ) */



	   /* Remove the remaining methods indent */
	theOutputDestination . removeIndent ( nameIndent );

	   /* End this statement. */
	theOutputDestination . write ( ";\n\n\n" );


      } /* if (  (theStatements.elementAt(i) instanceof DataSpawnTask      )
	 *     ||(theStatements.elementAt(i) instanceof DataWithDoStatement))*/

    } /* FOR ( i = 0;   i < theStatements.count();   i ++ ) */

  } /* public void generateHandleManagerDeclaration ( ... ) */




	/** This method walks the tree hierarchy to produce a list (vector)
	  * of every object in the tree hierarchy.  It makes use of
	  * (and returns) the instance-variable statementsVector.
	  *
	  * Copy the returned vector if you need to store it.  Successive
	  * calls will overwrite the data in the returned vector.
	  */
  public DataVector generateStatementsVector ( )
  {
	/* Migrated to DataStatement since it is now utilized for      *
	 * WITH-subsets in support of DISTRIBUTED ON-AGENT constraints */ 
    DataStatement.getChildrenInFlatVector ( statementsVector,
					    getTaskBody()     );

    return statementsVector;
  }



	/*
	 * Note:  This method generates (or regenerates) certain cached data.
	 * Specifically:  The 'statementsVector'and 'nonUniqueNamesVector'
	 * through generateStatementsVector() and itself.
	 *
	 * This method checks for invalid spawn-with constraints
	 * and duplicated labels.  Specifically, it checks for:
	 *   -  Duplicated labels.
	 *   -  labels & spawn-task-name duplicates that occur in with-clauses.
	 *   -  future-references & WAIT constraints combined.
	 */
  public DataVector validateTaskForCxxGeneration()  throws CompilationException
  {
    DataDestinationStringBuffer errorDestination
			= new DataDestinationStringBuffer();

    DataValidateCodeReturnValue    validationReturnValue
		        = new DataValidateCodeReturnValue ( errorDestination );

    DataVector  returnVector
              = validateTaskForCxxGeneration ( DataValidateCode.BASE_REFERENCE,
					       validationReturnValue );

	/* Did we find any errors? */
    if ( validationReturnValue . hasErrors() )
    {
      throw new CompilationException ( errorDestination . getString()
				       + "\n\n"
				       + validationReturnValue . toString() );
    }

	/* Return the list (Vector) of names that are not Unique. */
    return returnVector;
  }


  public DataVector validateTaskForCxxGeneration (
				    int                         theReference,
				    DataValidateCodeReturnValue theReturnValue)
    throws CompilationException
  {
	/*
	 * Notes:
        -* statements: list of every DataComponent inside & including this task
	 * taskRefsInWiths: list of all tags that are *USED* in WITH clauses.
	-* nonUniqueNames:  list of every name that occurs more than once.
	 *
	 * spawnHashtable:  ( dataSpawnTask.getTaskName(), dataSpawnTask )
	 *                   mapping of names to spawn-task objects.
	 *
	 * labelHashtable:  ( dataLabelStatement . getId(), dataStatement )
	 *                   mapping of names to statement objects.
	 *
	 * withsToComponents:
	 *             ( getConstraint(i).getEventTagTask(), getConstraint(i) )
	 *             Maps tag values in taskRefsInWiths to their
	 *             corresponding Constraint objects.  Also maps references
	 *             in constraint-statements to the constraint-statement.
	 *
	 * tdlBindsHashtable:  ( getTaskNameToBind(), DataBindTaskStatement )
	 *                      mapping of names to TDL_BIND statement objects.
	 *
	 * onAgentHashtable:  ( taskName / label , Integer( statementIndex ) )
	 *    mapping of on-agent task-names/labels to their corresponding
	 *    LATEST (LAST) on-agent constraint that occurs AT or PRIOR to
	 *    their SPAWN statement.  (AFTER on-agent constraints are ignored.)
	 *    (This is a semi-permanent fix until we can access tasks prior to
	 *     setting their on-agent value, which isn't likely to happen.)
	 */
    DataVector                  statements,
                                nonUniqueNames     = __nonUniqueNamesVector,
                                taskRefsInWiths    = __taskRefsInWiths,
				tmpVector          = new DataVector(1000);

    DataHashtable               labelHashtable     = __labelHashtable,
                                spawnHashtable     = __spawnHashtable,
				withsToComponents  = __withsToComponents,
				tdlBindsHashtable  = new DataHashtable(),
				onAgentHashtable   = __onAgentHashtable;

	/* kludgeWithDoStatements and kludgeSpawnStatements are a   *
	 * temporary fix until with-do-future-refs are implemented, *
	 * and to support onAgentHashtable generation.              */
    DataHashtable               kludgeWithDoStatements = new DataHashtable();
    DataHashtable               kludgeSpawnStatements  = new DataHashtable();
    DataHashtable               scratchHashtable       = new DataHashtable();
    int                         i, statementsIndex, tmpIndex;
    DataStatement               dataStatement;
    DataLabelStatement          dataLabelStatement;
    DataConstrainedObject       constrainedObject;
    DataSpawnTask               dataSpawnTask;
    DataBindTaskStatement       dataBindTaskStatement;
    DataConstraintStatement     dataConstraintStatement;
    boolean                     hasWait, hasFutureRef;
    String			waitLineNumber	    = null,
				futureRefLineNumber = null,
				taskTag;


	/* Start with empty lists... */
    nonUniqueNames    . removeAllElements();
    taskRefsInWiths   . removeAllElements();
    labelHashtable    . clear();
    spawnHashtable    . clear();
    withsToComponents . clear();

	/* Lets build up a complete (flat) listing of all the
	 * statements in this task body...
	 *
	 * (This is NOT the most efficient way of doing things...
	 *  But short of recursion...  Which is much *MUCH* messier...)
	 */
    statements = generateStatementsVector();



	/* If we have no task body, there is nothing to validate. */
	/* (We pass by default.  No errors exist to be found.)    */
    if ( getTaskBody() == null )
      return nonUniqueNames;



	/* PASS ONE:
	 *
	 * Set "taskRefsInWiths" and "withsToComponents" to be a
	 * listing of all the tasks (labels/task-names) that are
	 * referred to in spawn, with-do, or constraint statements
	 *
	 * And similarly for "tdlBindsHashtable",
	 * "kludgeWithDoStatements", and "kludgeSpawnStatements".
	 */
    for ( statementsIndex = 0;
	  statementsIndex < statements . count();
	  statementsIndex ++ )
    {
      dataStatement
	= (DataStatement) ( statements . elementAt ( statementsIndex ) );

	/* Is this a spawn/with-do/constraint statement? */
      if ( dataStatement instanceof DataConstrainedObject )
      {
	constrainedObject = (DataConstrainedObject) dataStatement;

	for ( i=0;  i < constrainedObject . getConstraintCount();  i++ )
	{
	  if ( constrainedObject . getConstraint ( i )
	                         . getHasNonStandardEventTagTask() )
	  {
	    taskTag
	      = constrainedObject . getConstraint ( i ) . getEventTagTask();

	    if ( withsToComponents . containsKey ( taskTag ) == false )
	    {
	      withsToComponents
		. put ( taskTag, constrainedObject . getConstraint ( i ) );
	      taskRefsInWiths . addElement ( taskTag );
	    }
	  }
	}
      } /* if ( dataStatement instanceof DataConstrainedObject ) */

	/* Include the initial reference in constraint-statements... */
      if ( dataStatement instanceof DataConstraintStatement )
      {
	taskTag = ((DataConstraintStatement) dataStatement) . getTaskTag();

	if (   ( withsToComponents . containsKey ( taskTag ) == false )
	    && ( taskTag . equals ( DataComponent.THIS )     == false ) )
	{
	  withsToComponents . put ( taskTag, dataStatement );
	  taskRefsInWiths . addElement ( taskTag );
	}
      }


	/* Create a list of all our DataBindTaskStatements,         *
	 * specifically so we can deal with ON_AGENT on a TDL_BIND. */
      if ( dataStatement instanceof DataBindTaskStatement )
      {
	tdlBindsHashtable . put ( ((DataBindTaskStatement) dataStatement)
				    . getTaskNameToBind(),
				  dataStatement );
      }



	/* Temporary kludge until with-do-future-refs are implemented */
      if ( dataStatement instanceof DataWithDoStatement )
      {
	for ( dataLabelStatement  = dataStatement . getLabel();
	      dataLabelStatement != null;
	      dataLabelStatement  = dataLabelStatement . getLabel() )
	{
	  if ( dataLabelStatement . hasId() )
	    kludgeWithDoStatements . put ( dataLabelStatement . getId(),
					   dataStatement );
	}
      }

	/* Temporary kludge until iteration-set-future-refs are implemented */
      if ( dataStatement instanceof DataSpawnTask )
      {
	for ( dataLabelStatement  = dataStatement . getLabel();
	      dataLabelStatement != null;
	      dataLabelStatement  = dataLabelStatement . getLabel() )
	{
	  if ( dataLabelStatement . hasId() )
	    kludgeSpawnStatements . put ( dataLabelStatement . getId(),
					  dataStatement );
	}

	kludgeSpawnStatements
	  . put ( ((DataSpawnTask) dataStatement) . getTaskName(),
		  dataStatement );
      }

    } /* FOR ( 0 <= statementsIndex < statements . count() ) */




	/* Pass TWO:
	 *
	 * Set up onAgentHashtable.
	 *
	 * Utilizes kludgeWithDoStatements / kludgeSpawnStatements,
	 * so we can't do it in Pass ONE.
	 */
    scratchHashtable . clear();

    for ( statementsIndex = 0;
	  statementsIndex < statements . count();
	  statementsIndex ++ )
    {
	/* Note:  This is NOT fixed.  It gets changed around below */
      dataStatement
	= (DataStatement) ( statements . elementAt ( statementsIndex ) );


      if ( dataStatement instanceof DataConstrainedObject )
      {
	constrainedObject = (DataConstrainedObject) dataStatement;

	for ( i=0;  i < constrainedObject . getConstraintCount();  i++ )
	{
	  if ( constrainedObject
	         . getConstraint    ( i )
	         . getConstraintType(   ) == DataConstraint.ON_AGENT )
	  {
	    if ( dataStatement instanceof DataConstraintStatement )
	    {
	      taskTag
		= ((DataConstraintStatement) dataStatement) . getTaskTag();

	      /* If the both contain the key, we have a collision that will
	       * be detected below.  For now, go with the WithDo statement.
	       */
	      if ( kludgeWithDoStatements . containsKey ( taskTag ) )
		dataStatement
		  = (DataStatement) kludgeWithDoStatements . get ( taskTag );

	      else if ( kludgeSpawnStatements . containsKey ( taskTag ) )
		dataStatement
		  = (DataStatement) kludgeSpawnStatements . get ( taskTag );

		/* If neither contain the key, we have an invalid task-tag,
		 * which will also be detected below.  Ignore for now.
		 */
	      else
		continue;

		/* If this With/Spawn statement has already transpired,
		 * we will detect it elsewhere (presently in DataStatement)
		 * as an overriden constraint.  Ignore for now.
		 */
	      if ( scratchHashtable . containsKey ( dataStatement ) )
		continue; /* Skip this constraint. */

	    } /* if ( dataStatement instanceof DataConstraintStatement ) */



	    if (   ( dataStatement instanceof DataSpawnTask       )
		|| ( dataStatement instanceof DataWithDoStatement ) )
	    {
		/* Just list the SPAWN statement in the first case.
		 * But flatten everything in the WITH-DO statement case.
		 */
	      DataStatement.getChildrenInFlatVector ( tmpVector,
						      dataStatement );

	      for ( tmpIndex = 0;
		    tmpIndex < tmpVector . count();
		    tmpIndex ++ )
	      {
		dataStatement = (DataStatement)
			        (tmpVector . elementAt (tmpIndex));

		 /* Insert into onAgentHashtable for each SPAWN we find */
		if ( dataStatement instanceof DataSpawnTask )
		{
		  for ( dataLabelStatement  = dataStatement . getLabel();
			dataLabelStatement != null;
			dataLabelStatement  = dataLabelStatement . getLabel() )
		  {
		    if ( dataLabelStatement . hasId() )
		      onAgentHashtable . put ( dataLabelStatement . getId(),
					    new Integer ( statementsIndex ) );
		  }

		  onAgentHashtable . put ( ((DataSpawnTask) dataStatement)
					      . getTaskName(),
					   new Integer ( statementsIndex ) );
		} /* if ( dataStatement instanceof DataSpawnTask ) */

	      } /* for ( 0 <= tmpIndex < tmpVector . count() */
	    } /* if ( dataStatement instanceof WITH or SPAWN statement ) */


	    else /* Idiocy Check against general weirdness. */
	    {
	      System.err.println (
		      "[DataTaskDefinition:validateTaskForCxxGeneration]  "
		      + "Warning:  Found DataConstrainedObject that is "
		      + "*NOT* a DataConstraintStatement, DataSpawnTask, "
		      + "or DataWithDoStatement:  "
		      + dataStatement . toString() );
	    }


		/* This statement is already flagged as being on-agent.      *
		 * We can safely ignore any other on-agent constraints here. */
	    break;

	  } /* if ( constrainedObject . getConstraint ( i ) == ON_AGENT ) */
	} /* for ( i=0;  i < constrainedObject . getConstraintCount(); i++ ) */
      } /* if ( dataStatement instanceof DataConstrainedObject ) */



	/* Note:  This was NOT fixed.  It may have been changed around. */
      dataStatement
	= (DataStatement) ( statements . elementAt ( statementsIndex ) );

	/* We need to be able to detect illegal on-agent constraints
	 * so we can ignore them.  Therefore, this cache.
	 */
      if (   ( dataStatement instanceof DataSpawnTask       )
	  || ( dataStatement instanceof DataWithDoStatement ) )
      {
	    /* Hopefully DataHashtable.get() is O(1), not O(N). */
	scratchHashtable . put ( dataStatement, dataStatement );
      }

    } /* FOR ( 0 <= statementsIndex < statements . count() ) */




	/* Everything should be in order here.  Lets verify stuff. */
    for ( statementsIndex = 0;
	  statementsIndex < statements . count();
	  statementsIndex ++ )
    {
      dataStatement
	= (DataStatement) ( statements . elementAt ( statementsIndex ) );

      dataSpawnTask = null;


	/* Is this a spawn statement */
      if ( dataStatement instanceof DataSpawnTask )
      {
	dataSpawnTask = (DataSpawnTask) dataStatement;

	    /* If there is another label with this task name, */
	if ( labelHashtable . containsKey ( dataSpawnTask . getTaskName() ) )
	{
	  nonUniqueNames . addElement ( dataSpawnTask . getTaskName() );

	    /* IF there is a with-clause that uses this name... */
	  if ( withsToComponents
	         . containsKey ( dataSpawnTask . getTaskName() ) )
	  {
	    theReturnValue
	      . addError ( 
	        ( (DataComponent)
		  (withsToComponents . get ( dataSpawnTask . getTaskName() ) )
	         ) )
	      . write ( "Ambiguous reference for name \"")
	      . write ( dataSpawnTask . getTaskName() )
	      . write ( "\". (Matches LABEL at line " )
	      . write (
	           ( (DataStatement)
		     ( labelHashtable . get ( dataSpawnTask . getTaskName() ) )
	            ) . getLineNumberString() )
	      . write ( " and SPAWN at line " )
	      . write ( dataSpawnTask . getLineNumberString() )
	      . write ( ".)\n" );
	  } /* IF ( dataSpawnTask . getTaskName() in WITH clause ) */
	} /* IF ( dataSpawnTask . getTaskName() in LABEL statement ) */


	    /* If we have NOT yet found a spawn-task with this name... */
	if ( spawnHashtable . containsKey ( dataSpawnTask . getTaskName() )
	     == false )
	{
	  spawnHashtable . put ( dataSpawnTask . getTaskName(),
				 dataSpawnTask );
	}
	else /* Have already found a spawn-task with this task-name */
	{
	  nonUniqueNames . addElement ( dataSpawnTask . getTaskName() );

	    /* IF there is a with-clause that uses this name... */
	  if ( withsToComponents
	         . containsKey ( dataSpawnTask . getTaskName() ) )
	  {
	    theReturnValue
	      . addError (
	        ( (DataComponent)
		  (withsToComponents . get ( dataSpawnTask . getTaskName() ) )
	         ) )
	      . write ( "Ambiguous reference for name \"")
	      . write ( dataSpawnTask . getTaskName() )
	      . write ( "\". (Matches SPAWN at line " )
	      . write (
	           ( (DataStatement)
		     ( spawnHashtable . get ( dataSpawnTask . getTaskName() ) )
	            ) . getLineNumberString() )
	      . write ( " and SPAWN at line " )
	      . write ( dataSpawnTask . getLineNumberString() )
	      . write ( ".)\n" );
	  } /* IF ( dataSpawnTask . getTaskName() in WITH clause ) */
	} /* ELSE ( dataSpawnTask . getTaskName() in ANOTHER SPAWN statement)*/

      } /* if ( dataStatement instanceof DataSpawnTask ) */



	    /* Handle TCM-Task-Binding Statements */
      if ( dataStatement instanceof DataBindTaskStatement )
      {
	dataBindTaskStatement = (DataBindTaskStatement) dataStatement;

	    /* If there is another spawn with this task name */
	if ( spawnHashtable . containsKey (
			dataBindTaskStatement . getTaskNameToBind() ) )
	{
	  theReturnValue
	    . addError ( dataBindTaskStatement )
	      . write ( "Ambiguous reference for name \"")
	      . write ( dataBindTaskStatement . getTaskNameToBind() )
	      . write ( "\".  (Matches SPAWN at line " )
	      . write (
	           ( (DataStatement)
		     spawnHashtable . get (
			  dataBindTaskStatement . getTaskNameToBind() )
	            ) . getLineNumberString() )
	      . write ( ".)\n" );
	}

	    /* If there is another label with this task name, */
	else if ( labelHashtable . containsKey ( 
			dataBindTaskStatement . getTaskNameToBind() ) )
	{

		/* Allow TDL_BIND's to supersede one another. */
	  if ( ! /*NOT*/ ( labelHashtable . get (
				  dataBindTaskStatement . getTaskNameToBind() )
			   instanceof DataBindTaskStatement ) )
	  {
	    theReturnValue
	      . addError ( dataBindTaskStatement )
	      . write ( "Ambiguous reference for name \"")
	      . write ( dataBindTaskStatement . getTaskNameToBind() )
	      . write ( "\".  (Matches LABEL at line " )
	      . write (
	           ( (DataStatement)
		     labelHashtable . get (
			  dataBindTaskStatement . getTaskNameToBind() )
	            ) . getLineNumberString() )
	      . write ( ".)\n" );
	  }
	}

	else
	{
	  labelHashtable . put (
			   dataBindTaskStatement . getTaskNameToBind(),
			   dataStatement );
	}

      } /* if ( dataStatement instanceof DataBindTaskStatement ) */



	/* Lets deal with the labels */
      for ( dataLabelStatement  = dataStatement . getLabel();
	    dataLabelStatement != null;
	    dataLabelStatement  = dataLabelStatement . getLabel() )
      {
		/* If this is NOT a case/default label... */
	if ( dataLabelStatement . hasId() )
	{
		/* Does this label duplicate a prior SPAWN-name.
		 * (Make an exception if it is this spawn, and the name
		 *  is unique so far...)
		 *  Ie:  foo: spawn foo(); 
		 */
	  if (   ( spawnHashtable
		    . containsKey ( dataLabelStatement . getId() ) )
		/* AND ( it is not this spawn AND the name is unique ) */
	      && (   ( dataSpawnTask == null )
		  || ( dataSpawnTask . getTaskName()
		         . equals ( dataLabelStatement . getId() )
		       == false )
		  || ( nonUniqueNames
		         . contains ( dataLabelStatement . getId() ) )
		  )
	      )
	  {
	    nonUniqueNames . addElement ( dataLabelStatement . getId() );

		/* IF there is a with-clause that uses this name... */
	    if ( withsToComponents
		   . containsKey ( dataLabelStatement . getId() ) )
	    {
	      theReturnValue
		. addError (
	          ( (DataComponent)
		    (withsToComponents . get( dataLabelStatement . getId() ) )
		   ) )
		. write ( "Ambiguous reference for name \"")
		. write (  dataLabelStatement . getId() )
		. write ( "\". (Matches SPAWN at line " )
		. write (
	             ( (DataSpawnTask)
		       ( spawnHashtable . get( dataLabelStatement . getId() ) )
		      ) . getLineNumberString() )
		. write ( " and LABEL at line " )
		. write ( dataLabelStatement . getLineNumberString() )
		. write ( ".)\n" );
	    } /* IF ( dataLabelStatement . getId() in WITH clause ) */
	  } /* IF ( LABEL is a prior spawn name ) */



		/* Do we do NOT have a duplicated label */
	  if ( labelHashtable . containsKey ( dataLabelStatement . getId() )
	       == false )
	  {
	    labelHashtable . put ( dataLabelStatement . getId(),
				   dataStatement );
	  }
	  else /* labelHashtable does contain dataLabelStatement */
	  {
	    nonUniqueNames . addElement ( dataLabelStatement . getId() );

		/*
		 * Duplicated labels are ALWAYS an error.
		 * (C++ compilers are *supposed* to barf on duplicated labels.)
		 */
		/* If duplicated label has been used in a with-constraint */
	    if ( withsToComponents
		   . containsKey ( dataLabelStatement . getId() ) )
	    {
	      theReturnValue
		. addError (
	          ( (DataComponent)
		    (withsToComponents . get( dataLabelStatement . getId() ) )
		   ) )
		. write ( "Ambiguous reference for name \"")
		. write (  dataLabelStatement . getId() )
		. write ( "\". (Matches LABEL at line " )
		. write (
	             ( (DataStatement)
		       ( labelHashtable . get( dataLabelStatement . getId() ) )
		      ) . getLineNumberString() )
		. write ( " and LABEL at line " )
		. write ( dataLabelStatement . getLineNumberString() )
		. write ( ".)\n" );
	    }
	    else
	    {
	      theReturnValue
		. addError ( dataLabelStatement )
		. write ( "LABEL \"" )
		. write (  dataLabelStatement . getId() )
		. write ( "\" duplicates LABEL at line " )
		. write (
	             ( (DataStatement)
		       ( labelHashtable . get( dataLabelStatement . getId() ) )
		      ) . getLineNumberString() )
		. write ( ".\n" );
	    }
	  } /* ELSE: Duplicated Label.. */


		/* If this label is used in a with-constraint
		 * and it is NOT labeling a spawn or with-do statement...
		 */
	  if (   ( withsToComponents
		     . containsKey ( dataLabelStatement . getId()         ) )
	      && ( /*NOT*/! ( dataStatement instanceof DataSpawnTask      ) )
	      && ( /*NOT*/! ( dataStatement instanceof DataWithDoStatement) ) )
	  {
	    theReturnValue
	      . addError ( dataLabelStatement )
	      . write ( "LABEL \"" )
	      . write (  dataLabelStatement . getId() )
	      . write ( "\" is used in constraint at line " )
	      . write (
	        ( (DataComponent)
		  (withsToComponents . get( dataLabelStatement . getId() ) )
	         ) . getLineNumberString() )
	      . write ( ", but does not refer to a SPAWN or WITH statement"
			+ ".\n" );
	  }

	} /* IF ( label . hasId() )  -- not case/default label... */
      } /* FOR ( dataLabelStatement's ) */



	/* Now we can deal with WAIT & future-references in DataSpawnTask's */
      if ( dataStatement instanceof DataSpawnTask )
      {
	hasWait      = false;
	hasFutureRef = false;

	for ( DataComponent datacomponent = dataStatement;
	      datacomponent              != null;
	      datacomponent               = datacomponent . getParent() )
	{
	  if ( datacomponent instanceof DataConstrainedObject )
	  {
	    constrainedObject = (DataConstrainedObject) datacomponent;

	    for ( i=0;   i < constrainedObject . getConstraintCount();   i++ )
	    {
	      if ( constrainedObject . getConstraint( i ) . getConstraintType()
		   == DataConstraint.WAIT )
	      {
		hasWait = true;
		waitLineNumber = constrainedObject . getConstraint ( i )
						   . getLineNumberString();
	      }

	      if (   ( constrainedObject . getConstraint ( i )
		                         . getHasNonStandardEventTagTask() )

	          && ( spawnHashtable . containsKey ( 
		         constrainedObject . getConstraint ( i )
			                   . getEventTagTask() ) == false )

	          && ( labelHashtable . containsKey ( 
		         constrainedObject . getConstraint ( i )
			                   . getEventTagTask() ) == false )
		  )
	      {
		hasFutureRef = true;
		futureRefLineNumber = constrainedObject . getConstraint ( i )
						       . getLineNumberString();
	      }
	    } /* FOR ( constrainedObject's constraints ) */
	  } /* IF ( DataConstrainedObject ) */
	} /* FOR ( This spawn object & all it's with-do parents... ) */

	if ( hasWait && hasFutureRef )
	{
	  theReturnValue
	    . addError ( dataStatement )
	    . write ( "WAIT constraint (line " )
	    . write ( waitLineNumber )
	    . write ( ") conflicts with Future-Reference (line " )
	    . write ( futureRefLineNumber )
	    . write ( ").\n" );
	}

      } /* IF ( dataStatement instanceof DataSpawnTask ) */



	/* Now we can deal with future/past reference constraints
	 * in DataConstraintStatement's...
	 */
      if ( dataStatement instanceof DataConstraintStatement )
      {
	dataConstraintStatement = ((DataConstraintStatement) dataStatement);

	    /* If this is a past-reference. */
	if (   ( spawnHashtable . containsKey (
				    dataConstraintStatement . getTaskTag() ) )
	    || ( labelHashtable . containsKey ( 
				    dataConstraintStatement . getTaskTag() ) )
	    )
	{
	  hasFutureRef = false;
	}
	else /* Must be a future-reference */
	{
	  hasFutureRef = true;
	}

	switch ( dataConstraintStatement
		   . getConstraint() . getConstraintType() )
	{
	  case DataConstraint.WAIT:
	    if ( hasFutureRef )
	    {
	      theReturnValue
		. addError ( dataStatement )
		. write ( "WAIT constraint applied against Future-Reference "
			  + "'" )
		. write ( dataConstraintStatement . getTaskTag() )
		. write ( "'.\n" );
	    }
	    break;

	  case DataConstraint.PARALLEL:
	  case DataConstraint.TERMINATE_AT_EVENT:
	  case DataConstraint.TERMINATE_AT_TIME:
	  case DataConstraint.TERMINATE_IN_TIME:
	  case DataConstraint.TERMINATE:
	  case DataConstraint.ACTIVATE_AT_EVENT:
	  case DataConstraint.ACTIVATE_AT_TIME:
	  case DataConstraint.ACTIVATE_IN_TIME:
	  case DataConstraint.ACTIVATE:
	  case DataConstraint.MAXIMUM_ACTIVATE:
	  case DataConstraint.MAXIMUM_TRIGGER:
	  case DataConstraint.MONITOR_PERIOD:
		/* No problems here -- as future or past references */
	    break;


	  case DataConstraint.EXPAND_FIRST:
	  case DataConstraint.DELAY_EXPANSION:
	  case DataConstraint.SEQUENTIAL_HANDLING:
	  case DataConstraint.SEQUENTIAL_EXPANSION:
	  case DataConstraint.SEQUENTIAL_EXECUTION:
	  case DataConstraint.SERIAL:
	  case DataConstraint.DISABLE_UNTIL_EVENT:
	  case DataConstraint.DISABLE_UNTIL_TIME:
	  case DataConstraint.DISABLE_FOR_TIME:
	    if ( hasFutureRef == false )
	    {
	      theReturnValue
		. addError ( dataStatement )
		. write ( "Constraint [" )
		. write ( dataConstraintStatement
			    . getConstraint() . getConstraintName() )
		. write ( "] applied against Task(s) ('" )
		. write ( dataConstraintStatement . getTaskTag() )
		. write ( "') that are already running.\n" );
	    }
	    break;


	  case DataConstraint.EXCEPTION_HANDLER:
	  case DataConstraint.ON_TERMINATE:
	    if ( hasFutureRef == false )
	    {
	      theReturnValue
		. addWarning ( dataStatement )
		. write ( "Constraint [" )
		. write ( dataConstraintStatement
			    . getConstraint() . getConstraintName() )
		. write ( "] applied against Task(s) ('" )
		. write ( dataConstraintStatement . getTaskTag() )
		. write ( "') that are already running.  " )
		. write ( "(Results are unpredictable.)\n" );
	    }
	    break;



	  case DataConstraint.ON_AGENT:
	    if ( tdlBindsHashtable . containsKey (
		   dataConstraintStatement . getTaskTag() ) )
	    {
	      theReturnValue
		. addError ( dataStatement )
		. write ( "Distributed '");
	      theReturnValue . getDataDestination()
			     . setStripLeadingWhitespace();
	      dataConstraintStatement . getConstraint()
		. generate ( theReturnValue . getDataDestination(),
			     DataComponent.ENTIRE_OBJECT );
	      theReturnValue . getDataDestination()
	        . write ( "' Constraint applied against " )
		. write ( "TDL_BIND-accessed Task ('" )
		. write ( dataConstraintStatement . getTaskTag() )
		. write ( "').\n" );
	    }
	    else if ( hasFutureRef == false )
	    {
	      theReturnValue
		. addError ( dataStatement )
		. write ( "Distributed '");
	      theReturnValue . getDataDestination()
			     . setStripLeadingWhitespace();
	      dataConstraintStatement . getConstraint()
		. generate ( theReturnValue . getDataDestination(),
			     DataComponent.ENTIRE_OBJECT );
	      theReturnValue . getDataDestination()
	        . write ( "' Constraint applied against " )
		. write ( "Task(s) ('" )
		. write ( dataConstraintStatement . getTaskTag() )
		. write ( "') that are already running.\n" );
	    }
	    break;



	  default:
	    if ( hasFutureRef == false )
	    {
	      theReturnValue
		. addWarning ( dataStatement )
		. write ( "Constraint [" )
		. write ( dataConstraintStatement
			    . getConstraint() . getConstraintName() )
		. write ( "] applied against Task(s) ('" )
		. write ( dataConstraintStatement . getTaskTag() )
		. write ( "') that are already running.  (Uncertain if " )
	        . write ( "this is a problem or not.  Possible TDLC " )
		. write ( " internal error.)\n" );
	    }
	    break;
	}
      } /* IF ( dataStatement instanceof DataConstraintStatement ) */


/*****************************************************************************/
	/* Future References that are With-Do statements are unimplemented */
	/* Also: Future References that are iteration-sets are unimplemented */
      if ( dataStatement instanceof DataConstrainedObject )
      {
	constrainedObject = (DataConstrainedObject) dataStatement;

	for ( i=0;   i < constrainedObject . getConstraintCount();   i++ )
	{
		/* Check Spawn Iteration-sets */
	  if (   ( constrainedObject . getConstraint ( i )
		                     . getHasNonStandardEventTagTask() )

	      && ( kludgeSpawnStatements . containsKey (
 			 constrainedObject . getConstraint ( i )
			                   . getEventTagTask() ) == true  )

	      && (   (   ( spawnHashtable . containsKey ( 
			     constrainedObject . getConstraint ( i )
			                       . getEventTagTask() ) == false )

		      && ( labelHashtable . containsKey ( 
		             constrainedObject . getConstraint ( i )
			                       . getEventTagTask() ) == false )
		      )

		  || ( checkSharedIterationParentIndexProblem (
			 dataStatement,
			 constrainedObject . getConstraint ( i ),
			 (DataComponent)
		          (kludgeSpawnStatements
			    . get ( constrainedObject . getConstraint ( i )
						      . getEventTagTask()  ) ),
			 theReturnValue )
		       == true )
		  )

	      && ( getIterationParentCount (
		       (DataComponent)
		        (kludgeSpawnStatements
			  . get ( constrainedObject . getConstraint ( i )
						    . getEventTagTask()  ) ) )
		   > 0 )

	      && (   (   constrainedObject
		           . getConstraint ( i )
		           . getHasEventTagTaskIndexes() == false
		      )
		  ||
		     (   getIterationParentCount (
		           (DataComponent)
			    (kludgeSpawnStatements
			      . get ( constrainedObject . getConstraint ( i )
						        . getEventTagTask()  )
			    ) )
		       >
			 constrainedObject
			   . getConstraint ( i )
			   . getEventTagTaskIndexes() . length
		      )
		  )
	      )
	  {
	    theReturnValue
	      . addError ( constrainedObject . getConstraint ( i ) )
	      . write ( "Constraint contains a Future-Reference on an " )
	      . write ( "Iteration-Set of Spawn Statements.  "          )
	      . write ( "(This is currently unimplemented.)\n"          );
	  }

		/* Check WithDo Iteration-sets */
	  else if (
		 ( constrainedObject . getConstraint ( i )
		                     . getHasNonStandardEventTagTask() )

	      && ( kludgeWithDoStatements . containsKey (
		         constrainedObject . getConstraint ( i )
			                   . getEventTagTask() ) == true  )

	      && (   (   ( spawnHashtable . containsKey ( 
			     constrainedObject . getConstraint ( i )
			                       . getEventTagTask() ) == false )

		      && ( labelHashtable . containsKey ( 
		             constrainedObject . getConstraint ( i )
			                       . getEventTagTask() ) == false )
		      )

		  || ( checkSharedIterationParentIndexProblem (
			 dataStatement,
			 constrainedObject . getConstraint ( i ),
			 (DataComponent)
		          (kludgeWithDoStatements
			    . get ( constrainedObject . getConstraint ( i )
						      . getEventTagTask()  ) ),
			 theReturnValue )
		       == true )
		  )

	      && ( getIterationParentCount (
		       (DataComponent)
		        (kludgeWithDoStatements
		          . get ( constrainedObject . getConstraint ( i )
						    . getEventTagTask()  ) ) )
		   > 0 )

	      && (   (   constrainedObject
		           . getConstraint ( i )
		           . getHasEventTagTaskIndexes() == false
		      )
		  ||
		     (   getIterationParentCount (
		           (DataComponent)
			    (kludgeWithDoStatements
			      . get ( constrainedObject . getConstraint ( i )
						        . getEventTagTask()  )
			    ) )
		       >
			 constrainedObject
			   . getConstraint ( i )
			   . getEventTagTaskIndexes() . length
		      )
		  )
	      )
	  {
	    theReturnValue
	      . addError ( constrainedObject . getConstraint ( i ) )
	      . write ( "Constraint contains a Future-Reference on an " )
	      . write ( "Iteration-Set of With Statements.  "           )
	      . write ( "(This is currently unimplemented.)\n"          );
	  }


		/* Check non-Iteration With-Do Statements */
	  else if (
		 ( constrainedObject . getConstraint ( i )
		                     . getHasNonStandardEventTagTask() )

	      && ( spawnHashtable . containsKey ( 
			 constrainedObject . getConstraint ( i )
			                   . getEventTagTask() ) == false )

	      && ( labelHashtable . containsKey ( 
		         constrainedObject . getConstraint ( i )
			                   . getEventTagTask() ) == false )

	      && ( kludgeWithDoStatements . containsKey (
		         constrainedObject . getConstraint ( i )
			                   . getEventTagTask() ) == true  )
	      )
	  {
	    theReturnValue
	      . addError ( constrainedObject . getConstraint ( i ) )
	      . write ( "Constraint contains a Future-Reference on a WITH " )
	      . write ( "statement.  (This is currently unimplemented.)\n"  );
	  }

	} /* FOR ( constrainedObject's constraints ) */
      } /* IF ( dataStatement instanceof DataConstrainedObject ) */
/*****************************************************************************/


/**********************************************************************
**********  Check on accessing a distribted (on-agent)        *********
**********  Task-ref before the on-agent AGENT is finalized.  *********
***********************************************************************/

	/* Check any tag-tasks we are using inside a constraint. */
      if ( dataStatement instanceof DataConstrainedObject )
      {
	constrainedObject = (DataConstrainedObject) dataStatement;

	for ( i=0;  i < constrainedObject . getConstraintCount();  i++ )
	{
	  if ( constrainedObject . getConstraint    ( i )
		                 . getHasNonStandardEventTagTask() )
	  {
	    taskTag = constrainedObject . getConstraint ( i )
			                . getEventTagTask();

	    if (   ( onAgentHashtable . containsKey ( taskTag )
		    )
	        && (   ( (Integer) onAgentHashtable . get ( taskTag )
			) . intValue()
		     > statementsIndex
		    )
	        )
	    {
	      theReturnValue
		. addError ( constrainedObject . getConstraint ( i )         )
		. write ( "Constraint contains a future reference ['"        )
		. write ( taskTag                                            )
		. write ( "'] to a Distributed Task prior to that Task''s "  )
		. write ( "final ON-AGENT constraint [line "                 )
		. write ( ( (DataStatement) ( statements . elementAt ( 
			      ( (Integer) onAgentHashtable . get ( taskTag )
		               ) . intValue() ) )
			   ) . getLineNumberString()                         )
		. write ( "].  Future references prior to "                  )
		. write ( "the final ON-AGENT constraint are not currently " )
		. write ( "supported.  Recommended workaround is to utilize ")
		. write ( "an ON-AGENT Constraint-Statement for Task '"      )
		. write ( taskTag                                            )
		. write ( "' prior to this statement.\n"                     );
	    }
	  }
	} /* for ( i=0;  i < constrainedObject . getConstraintCount();  i++ )*/
      } /* if ( dataStatement instanceof DataConstrainedObject ) */




	/* Search for TDL_REF() task-tags to check. */
	/* statementsIndex == 0 corresponds to the entire task body. */
      if ( statementsIndex > 0 )
      {
	dataStatement . runOnSubcomponentFraction (
	  "TDL_REF",
	  this,
	  new RunOnSubcomponent_TdlRefDistributedData ( statementsIndex,
							statements,
							onAgentHashtable,
							theReturnValue ) );
      }

/**********************************************************************
**********  End: Check on accessing a distribted (on-agent)        ****
**********       Task-ref before the on-agent AGENT is finalized.  ****
***********************************************************************/



    } /* FOR ( 0 <= statementsIndex < statements . count() ) */


	/* Deal with missing tasks... */
    for ( i=0;  i < taskRefsInWiths.count();  i++ )
    {
      taskTag = (String) ( taskRefsInWiths . elementAt ( i ) );
      
      if (   ( spawnHashtable . containsKey ( taskTag ) == false )
	  && ( labelHashtable . containsKey ( taskTag ) == false ) )
      {
	theReturnValue
	  . addError ( ( (DataComponent) (withsToComponents . get( taskTag ))
			) )
	  . write ( "Unknown reference for name '")
	  . write (  taskTag )
	  . write ( "'.\n" );
      }
    }


    return nonUniqueNames;
  }



	/* RunOnSubcomponentObject interface -- used as callback when
	 * finding TDL_REF for distributed-conflict checking in
	 * validateTaskForCxxGeneration() up above.
	 */
  public void foundSubcomponentMatch (
			      String                   theString,
			      String                   theStringToSearchIn,
			      int                      theStringIndexOfMatch,
			      DataComponent            theDataComponent,
			      int                      theSubcomponentIndex,
			      Object                   theArgumentObject )
  {
    String                      tdlRefTaskName;
    int                         statementsIndex;
    DataVector                  statements;
    DataHashtable               onAgentHashtable;
    DataValidateCodeReturnValue theReturnValue;


    if ( theString . equals ( "TDL_REF" ) == false )
    {
      System.err.println ( "[DataTaskDefinition:foundSubcomponentMatch]  "
			   + "Error:  Unexpected search string:  \""
			   + theString + "\"." );
      return;
    }

	/* Unpack our arguments... */
    statementsIndex
      = ((RunOnSubcomponent_TdlRefDistributedData) theArgumentObject)
            . getStatementsIndex();

    statements
      = ((RunOnSubcomponent_TdlRefDistributedData) theArgumentObject)
            . getStatements();

    onAgentHashtable
      = ((RunOnSubcomponent_TdlRefDistributedData) theArgumentObject)
            . getOnAgentHashtable();

    theReturnValue
      = ((RunOnSubcomponent_TdlRefDistributedData) theArgumentObject)
            . getReturnValue();



	/* Searching on "TDL_REF ( foo )", we want to extract "foo". */

	/* Start with a string containing everything from this point on. */
    theStringToSearchIn
      =  theStringToSearchIn . substring ( theStringIndexOfMatch );

    if ( theDataComponent != null )
    {
      StringBuffer tmpStringBuffer = new StringBuffer ( 1000 );

      tmpStringBuffer . append ( theStringToSearchIn );

      for ( theSubcomponentIndex++;
	    theSubcomponentIndex < theDataComponent . getSubcomponentsCount();
	    theSubcomponentIndex ++ )
      {
	tmpStringBuffer
	  . append ( theDataComponent
		       . getSubcomponent ( theSubcomponentIndex )
		       . toString() );
      }

      theStringToSearchIn = tmpStringBuffer . toString();
    }


    TDLParser . reinitParser ( theStringToSearchIn );

    try
    {
      tdlRefTaskName = TDLParser . getParser() . parseTdlRefTaskName();
    }
    catch ( Throwable  theExceptionOrError )
    {
      System.err.println ( "[DataTaskDefinition:foundSubcomponentMatch]  "
			   + "Error:  Unexpected error encountered while "
			   + "trying to re-parse out TDL_REF Task-name:  "
			   + theExceptionOrError.toString() );
      return;
    }
    
    if (   ( tdlRefTaskName . equals ( "THIS" ) == false
	    )
        && ( onAgentHashtable . containsKey ( tdlRefTaskName )
	    )
	&& (   ( (Integer) onAgentHashtable . get ( tdlRefTaskName )
		) . intValue()
	    >= statementsIndex
	   )
	)
    {
      theReturnValue
	. addError ( ( (DataStatement)
		       ( statements . elementAt ( statementsIndex ) )
		      )                                                     )
	. write ( "TDL_REF("                                                )
	. write ( tdlRefTaskName                                            )
	. write ( ") contains a future reference [\""                       )
	. write ( tdlRefTaskName                                            )
	. write ( "\"] to a Distributed Task prior to that Task''s "        )
	. write ( "final ON-AGENT constraint [line "                        )
	. write ( ( (DataStatement) ( statements . elementAt ( 
		      ( (Integer) onAgentHashtable . get ( tdlRefTaskName )
	               ) . intValue() ) )
		   ) . getLineNumberString()                                )
	. write ( "].  Future references prior to "                         )
	. write ( "the final ON-AGENT constraint are not currently "        )
	. write ( "supported.  Recommended workaround is to utilize "       )
	. write ( "an ON-AGENT Constraint-Statement for Task \""            )
	. write ( tdlRefTaskName                                            )
	. write ( "\" prior to this statement.\n"                           );
    }
  }





	/** Accesses the most recently "generated" (cached) statementsVector */
  public DataVector getCachedStatementsVector ( )
  {
    return statementsVector;
  }


	/** Accesses the most recently "generated" (cached) onAgentHashtable */
  public DataHashtable getCachedOnAgentHashtable ( )
  {
    return __onAgentHashtable;
  }


	/** Accesses the most recently "generated" (cached) data */
  public int getDataSpawnTaskLocalOrDistributed (
					DataSpawnTask theDataSpawnTask )
  {
    return getDataSpawnTaskLocalOrDistributed ( theDataSpawnTask,
						getCachedOnAgentHashtable(),
						getCachedStatementsVector() );
  }

  public int getDataSpawnTaskLocalOrDistributed (
					   DataSpawnTask   theDataSpawnTask,
					   DataHashtable   theOnAgentHashtable,
					   DataVector      theStatements )
  {
    if ( theOnAgentHashtable . containsKey ( theDataSpawnTask.getTaskName() ) )
    {
      if ( DataStatement.hasNonconditionalConstraintOfType (
						      theDataSpawnTask,
						      DataConstraint.ON_AGENT,
						      theStatements ) )
	return DataComponent.DISTRIBUTED_ONLY;
      else
	return DataComponent.EITHER_LOCAL_OR_DISTRIBUTED;
    }
    else
      return DataComponent.LOCAL_NONDISTRIBUTED_ONLY;
  }





	/*** Uses cached data generated by  validateTaskForCxxGeneration() ***/
  public DataComponent getDataComponentWithName ( String theName )
  {
    if ( __spawnHashtable . containsKey ( theName ) )
      return (DataComponent) ( __spawnHashtable . get( theName ) );

    if ( __labelHashtable . containsKey ( theName ) )
      return (DataComponent) ( __labelHashtable . get( theName ) );

    return null;
  }



  public int getIterationParentCount ( DataComponent theDataComponent )
    throws CompilationException
  {
    DataComponent ancestor;
    int           iterationCount = 0;

	/* Find out how many iteration loops enclose the theDataComponent */
    for ( ancestor  = theDataComponent;
	  ancestor != null;
	  ancestor  = ancestor . getParent()
	 )
    {
      if ( ancestor instanceof DataIterationStatement )
	iterationCount++;

      if ( ancestor instanceof DataTaskDefinition )
	break;
    }

    if ( ancestor == null )
    {
      throw new CompilationException (
	       theDataComponent.getMessageFilenameLead()
	     + theDataComponent.getLineNumberString()
	     + ":  Programmer Error:  Unable to find tag-Task-DataComponent's "
	     + "DataTaskDefinition ancestor." );
    }

    return iterationCount;
  }


	/* Internal method to deal with a special case of Future-references
	 * on iteration sets. */
  protected boolean checkSharedIterationParentIndexProblem (
				 DataComponent               theDataComponent,
				 DataConstraint              theDataConstrant,
				 DataComponent               theTargetSpawn,
				 DataValidateCodeReturnValue theReturnValue  )
    throws CompilationException
  {
    DataComponent  constraintAncestor, spawnAncestor;
    int            iterationCount = 0;

    for ( constraintAncestor  = theDataComponent;
	  constraintAncestor != null;
	  constraintAncestor  = constraintAncestor . getParent()
	 )
    {
      iterationCount = 0;

      if ( constraintAncestor instanceof DataTaskDefinition )
	break;

      for ( spawnAncestor  = theTargetSpawn;
	    spawnAncestor != null;
	    spawnAncestor  = spawnAncestor . getParent()
	   )
      {
	if ( spawnAncestor == constraintAncestor )
	  break;

	if ( spawnAncestor instanceof DataIterationStatement )
	  iterationCount++;

	if ( spawnAncestor instanceof DataTaskDefinition )
	  break;
      } /* FOR ( spawnAncestor ) */

      if ( spawnAncestor == null )
      {
	throw new CompilationException (
	       theDataComponent.getMessageFilenameLead()
	     + theDataComponent.getLineNumberString()
	     + ":  PROGRAMMER Error:  Unable to find tag-Task-DataComponent's "
	     + "DataTaskDefinition ancestor." );
      }

      if ( spawnAncestor == constraintAncestor )
	break;

    } /* FOR ( constraintAncestor ) */

    if ( constraintAncestor == null )
    {
      throw new CompilationException (
	       theDataComponent.getMessageFilenameLead()
	     + theDataComponent.getLineNumberString()
	     + ":  PROGRAMMER ERROR:  Unable to find DataComponent's "
	     + "DataTaskDefinition ancestor." );
    }

	/* Trival case.  No overlap */
    if ( constraintAncestor instanceof DataTaskDefinition )
      return false;

	/* Non-trival case.   Both are contained in at least some of the
	 * same iteration loops.
	 */
    if (   (   getIterationParentCount ( theTargetSpawn )
	     - iterationCount
	    )
	 > (   ( theDataConstrant . getHasEventTagTaskIndexes() == true )
	     ?   theDataConstrant . getEventTagTaskIndexes() . length
	     :   0
	    )
	)
    {
	/* Definite problem */
      return true;
    }
    else
    {
	/* Assume that they do not refer to future references... */
/*** TODO: Comment this out for now...
      theReturnValue
	. addWarning ( theDataConstrant )
	. write ( "Possible future reference on an Iteration-Set.  " )
	. write ( "Assuming you know what you are doing...\n"        );
***/
      return false;
    }
  }



	/** This will return a string that uniquely identifies this subtask.
	  * (Typically, the subtask name followed by the number of times 
	  *  that that name has been previously used.)
	  */
  public String getIdentifierForSubtask ( DataSpawnTask  theDataSpawnTask )
  {
    DataVector  statements = getCachedStatementsVector();
    int         i, spawnCount = 0;

    for ( i=0;   i < statements . count();   i++ )
    {
      if ( statements . elementAt ( i ) == theDataSpawnTask )
      {
	break;
      }
    }

    if ( i >= statements . count() )
    {
      System.err.println ( "[DataTaskDefinition:getIdentifierForSubtask]  "
			   + "Error:  theDataSpawnTask ("
			   + theDataSpawnTask . getTaskName()
			   + ") is not inside this Task." );
      return theDataSpawnTask . getTaskName() + "_0x"
	+ Integer.toHexString ( theDataSpawnTask . hashCode() );
    }

	/* Increment spawnCount only for spawns with matching names */
    for ( i-- ;   i >=0;   i-- )
    {
      if (   ( statements . elementAt ( i ) instanceof DataSpawnTask )
	  && ( ((DataSpawnTask) ( statements . elementAt ( i ) ))
	       . getTaskName() . equals ( theDataSpawnTask . getTaskName() ) ))
      {
	spawnCount ++;
      }
    }

    return theDataSpawnTask . getTaskName() + "-" + spawnCount;
  }



	/** This will return a string that uniquely identifies this branch.
	  * (Typically, "WithDo-" followed by the number of prior WITH-DO
	  *  statements.
	  */
  public String getIdentifierForBranch (
				  DataWithDoStatement  theDataWithDoStatement )
  {
    DataVector    statements = getCachedStatementsVector();
    int           i, withDoCount = 0;
    final String  baseString = "WithDo-";

    for ( i=0;   i < statements . count();   i++ )
    {
      if ( statements . elementAt ( i ) == theDataWithDoStatement )
      {
	break;
      }
    }

    if ( i >= statements . count() )
    {
      System.err.println ( "[DataTaskDefinition:getIdentifierForBranch]  "
		 + "Error:  theDataWithDoStatement is not inside this Task:"
		 + theDataWithDoStatement );
      return baseString + "_0x"
	+ Integer.toHexString ( theDataWithDoStatement . hashCode() );
    }

    for ( i-- ;   i >=0;   i-- )
    {
      if ( statements . elementAt ( i ) instanceof DataWithDoStatement )
      {
	withDoCount ++;
      }
    }

    return baseString + withDoCount;
  }



	/** This will return a unique index of this constraint in this Task.
	  * It relies on getCachedStatementsVector() being set up properly.
	  * It returns DataComponent.UNKNOWN_CONSTRAINT_INDEX if
	  *   theDataConstraint is not found in this Task.
	  * If theInvertOrder is true, the last constraint is 1,
	  *   and the first constraint is totalConstraints.
	  */
  public int getIdentifierForConstraint ( DataConstraint theDataConstraint )
	    { return getIdentifierForConstraint ( theDataConstraint, false ); }

  public int getIdentifierForConstraint ( DataConstraint theDataConstraint,
					  boolean        theInvertOrder    )
  {
    DataVector    statements = getCachedStatementsVector();
    int           i, j, constraintCount = 0, totalConstraints = 0;
    boolean       foundConstraint = false;

    for ( i=0;   i < statements . count();   i++ )
    {
      if ( statements . elementAt ( i ) instanceof DataConstrainedObject )
      {
	totalConstraints
	  += ((DataConstrainedObject) (statements . elementAt ( i )))
		 . getConstraintCount();

	if ( foundConstraint == false )
	{
	  for ( j = 0;
		j < ( ((DataConstrainedObject) (statements . elementAt ( i )))
		         . getConstraintCount() );
		j++ )
	  {
	    if ( ((DataConstrainedObject) (statements . elementAt ( i )))
	            . getConstraint ( j ) == theDataConstraint )
	    {
	      foundConstraint = true;
	      break;
	    }
	    else
	    {
	      constraintCount++;
	    }
	  } /* FOR ( 0 <= j < getConstraintCount() ) */
	} /* if ( foundConstraint == false ) */
      } /* if ( statements.elementAt (i) instanceof DataConstrainedObject ) */
    } /* for ( i=0;   i < statements . count();   i++ ) */

    if ( foundConstraint == false )
      return DataComponent.UNKNOWN_CONSTRAINT_INDEX;

    if ( theInvertOrder == false )
      return constraintCount;
    else
      return totalConstraints - constraintCount;
  }

}
