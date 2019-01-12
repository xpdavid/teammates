/**
 * The intent of calling the Rest API.
 */
export enum Intent {

  /**
   * To get the full detail of the entities.
   */
  FULL_DETAIL = 'FULL_DETAIL',

  /**
   * To submit the feedback session as instructors.
   */
  INSTRUCTOR_SUBMISSION = 'INSTRUCTOR_SUBMISSION',

  /**
   * To submit the feedback session as students.
   */
  STUDENT_SUBMISSION = 'STUDENT_SUBMISSION',

  /**
   * To moderate the feedback submission of a student.
   */
  INSTRUCTOR_MODERATE_STUDENT_SUBMISSION = 'INSTRUCTOR_MODERATE_STUDENT_SUBMISSION',

  /**
   * To moderate the feedback submission of a instructor.
   */
  INSTRUCTOR_MODERATE_INSTRUCTOR_SUBMISSION = 'INSTRUCTOR_MODERATE_INSTRUCTOR_SUBMISSION',
}
