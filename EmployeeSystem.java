import java.util.Scanner;

class Employee 
{
	private String employeeId;
    	private String employeeName;
    	private int age;
    	private String department;
    	private String designation;
    	private double baseSalary;
    	private int daysPresent;
    	private int leaveBalance;
    	private double loanAmount;
    	private String dailyAttendanceStatus;

    	public Employee(String employeeId, String employeeName, int age, String department,String designation, double baseSalary, int leaveBalance)
	{

        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.designation = designation;
        this.daysPresent = 30;
        this.loanAmount = 0.0;
        this.dailyAttendanceStatus = "NOT MARKED";

        if (age >= 18) 
	{
            this.age = age;
        }//if

	else 
	{
            this.age = 18;
            System.out.println("Warning: Invalid age provided. Defaulted to 18.");
        }//else

        if (baseSalary >= 0)
	{
            this.baseSalary = baseSalary;
        } //if
	
	else
	{
            this.baseSalary = 0.0;
            System.out.println("Warning: Base salary cannot be negative.");
        }//else

        if (leaveBalance >= 0) 
	{
            this.leaveBalance = leaveBalance;
        }//if

	else 
	{
            this.leaveBalance = 0;
            System.out.println("Warning: Leave balance cannot be negative.");
        }//else
    	}//public emp

// Daily Attendance Methods
	public void markPresent() 
	{
        this.dailyAttendanceStatus = "PRESENT";
        System.out.println("Status Updated: Employee marked as PRESENT for today.");
    	}//void makepresent()

    	public void markAbsent() 
	{
        this.dailyAttendanceStatus = "ABSENT";
        System.out.println("Status Updated: Employee marked as ABSENT for today.");
    	}//void markAbsent()

// Monthly Absence Recording & Salary Logic
    	public void recordMonthlyAbsences(int daysAbsent)
	{
        	if (daysAbsent < 0)
		{
           		 System.out.println("Error: Days absent cannot be negative.");
        	}//if
		
		else if (daysAbsent > 30) 
		{
           		 System.out.println("Error: Absences cannot exceed 30 days in a month.");
       		
		}//else if
		
		else 	
		{
            		this.daysPresent = 30 - daysAbsent;
            		System.out.println("Successfully recorded " + daysAbsent + " absent day(s) for the month.");
            		System.out.println("Updated Monthly Attendance: " + daysPresent + "/30 days present.");
        	}//else
    	}//void recordMonthlyAbsences

    	public double calculateNetSalary() 
	{
        
	if (daysPresent < 27) 
	{
            int extraAbsentDays = 27 - daysPresent;
            double dailyRate = baseSalary / 30.0;
            return baseSalary - (extraAbsentDays * dailyRate);
        }//if

        return baseSalary;
    	}//calculateNetSalary() 
	
// Leave Management

	public void applyLeave(int days)
	{

        if (days <= 0)
	{
            System.out.println("Error: Leave days must be positive.");
        }//if
	
	else if (days > leaveBalance)
	{
            System.out.println("Error: Insufficient leave balance! Available Leave: " + leaveBalance);
        }//else if 

	else
	{
            leaveBalance -= days;
            System.out.println("Successfully approved leave for " + days + " day(s).");
            System.out.println("Remaining Leave Balance: " + leaveBalance);
        }//else
	}//void applyLeave()

// Loan Management
	
	public void takeLoan(double amount)
	{
        	if (amount <= 0)	
		{
            		System.out.println("Error: Loan amount must be positive.");
        	}//if 
		
		else
		{
           		loanAmount += amount;
            		System.out.println("Successfully approved company loan of Rs." + amount);
            		System.out.println("Updated Total Loan Amount: Rs." + loanAmount);
        	}//else
   	 }//void loantaken()

   	public void printLoanStatement() 
	{
        	System.out.println("\n---------------------------------");
        	System.out.println("      EMPLOYEE LOAN STATEMENT    ");
        	System.out.println("---------------------------------");
        	System.out.println("Employee ID   : " + employeeId);
       		System.out.println("Employee Name : " + employeeName);
        	System.out.println("Age           : " + age);
        	System.out.println("Total Loan    : Rs." + loanAmount);
        	System.out.println("---------------------------------");
  	}//void printLoanStatement() 
	
// Full Details Display
    
	public void displayDetails() 
	{
        	int daysAbsent = 30 - daysPresent;
        	double netSalary = calculateNetSalary();

        	System.out.println("\n---------------------------------");
        	System.out.println("    COMPLETE EMPLOYEE DETAILS    ");
        	System.out.println("--------------------------------------");
        	System.out.println("Employee ID          : " + employeeId);
        	System.out.println("Employee Name        : " + employeeName);
        	System.out.println("Age                  : " + age);
        	System.out.println("Department           : " + department);
       		System.out.println("Designation          : " + designation);
        	System.out.println("Today's Status       : " + dailyAttendanceStatus);
        	System.out.println("-----------------------------------");
        	System.out.println("Base Salary          : Rs." + baseSalary);
        	System.out.println("Days Present (Month) : " + daysPresent + "/30");
        	System.out.println("Days Absent (Month)  : " + daysAbsent);
        	System.out.println("Calculated Net Salary: Rs." + String.format("%.2f", netSalary));
        	System.out.println("------------------------------------");
        	System.out.println("Leave Balance        : " + leaveBalance + " days");
        	System.out.println("Company Loan Balance : Rs." + loanAmount);
        	System.out.println("-----------------------------------");
    	}//void displayDetails()
}//class Employee()


public class EmployeeSystem 
{
	public static void main(String[] args) 
	{
        	Scanner scanner = new Scanner(System.in);

        	System.out.println("==============================================");
        	System.out.println("    WELCOME TO EMPLOYEE MANAGEMENT SYSTEM    ");
        	System.out.println("==============================================");

        	System.out.println("Enter Employee ID: ");
        	String empId = scanner.nextLine();

        	System.out.println("Enter Employee Name: ");
        	String empName = scanner.nextLine();

        	System.out.println("Enter Employee Age: ");
        	int age = scanner.nextInt();
        	scanner.nextLine(); // Consume newline leftover from nextInt()

        	System.out.println("Enter Department: ");
        	String department = scanner.nextLine();

        	System.out.println("Enter Designation: ");
        	String designation = scanner.nextLine();

        	System.out.println("Enter Base Salary: ");
        	double baseSalary = scanner.nextDouble();

        	System.out.println("Enter Initial Leave Balance: ");
        	int leaveBalance = scanner.nextInt();

        	Employee employee = new Employee(empId, empName, age, department, designation, baseSalary, leaveBalance);
        	System.out.println("\nEmployee record created successfully!");

        	boolean isRunning = true;
        	while (isRunning)
		{
            		System.out.println("\n=== MAIN MENU ===");
            		System.out.println("1. Mark Daily Present");
            		System.out.println("2. Mark Daily Absent");
            		System.out.println("3. Record Total Monthly Absences");
            		System.out.println("4. Apply for Leave");
           		System.out.println("5. Take Company Loan");
            		System.out.println("6. Print Loan Statement");
            		System.out.println("7. Display Details & Net Salary");
            		System.out.println("8. Exit");
            		System.out.println("Enter choice (1-8): ");

            		int choice = scanner.nextInt();

            		switch (choice) 
			{
                		case 1:
                   			employee.markPresent();
                    			break;

               			case 2:
                    			employee.markAbsent();
                   			break;

                		case 3:
                    			System.out.println("Enter total days absent in the month: ");
                    			int absentDays = scanner.nextInt();
                    			employee.recordMonthlyAbsences(absentDays);
                    			break;

                		case 4:
                    			System.out.println("Enter number of leave days to apply: ");
                    			int leaveDays = scanner.nextInt();
                   			employee.applyLeave(leaveDays);
                   			break;

                		case 5:
                    			System.out.println("Enter loan amount requested: ");
                    			double loanAmt = scanner.nextDouble();
                    			employee.takeLoan(loanAmt);
                    			break;

               			case 6:
                    			employee.printLoanStatement();
                    			break;

                		case 7:
                    			employee.displayDetails();
                    			break;

                		case 8:
                   			System.out.println("\nThank you for using Employee Management System. Have a great day ahead!");
                    			isRunning = false;
                    			break;

                		default:
                   		System.out.println("Error: Invalid choice! Select between 1 and 8.");
           		}//switch
       		}//while

        scanner.close();
    }//main()
}//class EmployeeSystem