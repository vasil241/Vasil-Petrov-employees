import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        String file = "employees.csv";
        BufferedReader reader = null;
        String line = "";
        List<String[]> data = new ArrayList<>();

        // add the content of the .csv file to a list data structure
        // each record (row) in the csv file will be an element in the list
        try {
            reader = new BufferedReader(new FileReader(file));
            while((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                data.add(row);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }

        // Create a Map<projectId, list of employees>> , we are grouping the employees by project id
        // the list will contain all the employees involved with the project together with their start and end date
        // we know that each record will hold 4 String values (EmpId, ProjId, StartDate, EndDate)
        Map<String, List<String[]>> projects = new HashMap<>();
        for(String[] record : data) {
            if(projects.containsKey(record[1])) {
                // add another employee to the project's list of employees
                // project id already in map, we add another employee to its list with [empId, startDate, endDate]
                projects.get(record[1]).add(new String[] {record[0], record[2], record[3]});
            } else {
                // add another project to the 'projects' map
                List<String[]> employees = new ArrayList<>();
                employees.add(new String[] {record[0], record[2], record[3]});
                projects.put(record[1], employees);
            }
        }
        
        // print the created map containing all the unique projects and the employees associated with them
        for(String key : projects.keySet()) {
            System.out.print("Project ID " + key + "\t");
            for(String[] emp : projects.get(key)) {
                System.out.print(Arrays.toString(emp) + "\t");
            }
            System.out.println();
        }

        // Create a Map<pair of employee ids, startDate - endDate that they've worked together on the project>
        // the values of the map will be represented as a list containing String[] in the format [projectId, startDate, endDate, days worked] 
        // so to say here we are creating the pairs of employees that have worked together on different projects
        Map<String, List<String[]>> pairs = new HashMap<>();
        for(String key : projects.keySet()) {
            List<String[]> employees = projects.get(key);
            for(int i = 0; i < employees.size(); i++) {
                // here we pick up each employee with its start and end date on the specific project
                // and we search if it has overlapping days (dates) with another employee that has worked on the same project
                String[] emp1 = employees.get(i); // [empID, startDate, endDate]
                for(int j = i+1; j < employees.size(); j++) {
                    String[] emp2 = employees.get(j);
                    String[] daysWorked = calculateDaysWorked(key, emp1[1], emp2[1], emp1[2], emp2[2]);
                    if(daysWorked.length == 0) 
                        continue;
                    String pair = emp1[0] + "," + emp2[0];
                    if(pairs.containsKey(pair)) {
                        pairs.get(pair).add(daysWorked);
                    } else {
                        pairs.put(pair, new ArrayList<String[]>());
                        pairs.get(pair).add(daysWorked);
                    }
                }
            }
        }
        
        System.out.println();
        for(String key : pairs.keySet()) {
            System.out.print("Employee pair " + key + "\t");
            for(String[] project : pairs.get(key)) {
                System.out.print(Arrays.toString(project) + "\t");
            }
            System.out.println();
        }

        // this map will contain a pair of employees that have worked together and the total days of their work
        // if they have worked on multiple projects with overlapping days, the days will be counted only once
        // for example if Emp A and B have worked together on both project 1 and 2 during June, the total days will be 30, not 60 
        Map<String, Integer> total_days_per_pair = calculatePairTotalDays(pairs);

        System.out.println();
        for(String key : total_days_per_pair.keySet()) {
            System.out.println("Employee pair " + key + "\t Total days worked together: " + total_days_per_pair.get(key));
        }

        // return the pair of employees that have worked together the most with total days
        int max = 0;
        String pair = "";
        for(String key : total_days_per_pair.keySet()) {
            if(max < total_days_per_pair.get(key)) {
                max = total_days_per_pair.get(key);
                pair = key;
            }
        }

        // print the final result
        System.out.println("\nThe employees with ids " + pair + " have worked the most time together, " + max + " days in total");
    }

    private static String[] calculateDaysWorked(String projectId, String startDate1, String startDate2, String endDate1, String endDate2 ) throws ParseException {
        // now from this method we want to get the startDate that is bigger and the endDate that is smaller
        // for example if emp1 [2, 6.12.2013, 6.10.2014] and emp2 [6, 15.3.2014, 16.3.2014], result should be [15.3.2014, 16.3.2014]
        // the result returns the dates on which the 2 employees have worked together on a specific project
        // let's stick to the date format that was given to us via the .csv file
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date start1 = format.parse(startDate1);
        Date start2 = format.parse(startDate2);
        Date end1 = endDate1.equals("NULL") ? new Date() : format.parse(endDate1);
        Date end2 = endDate2.equals("NULL") ? new Date() : format.parse(endDate2);

        // find the largest start date
        Date largestStart = start1.getTime() > start2.getTime() ? start1 : start2;
        // find the smallest end date
        Date smallestEnd = end1.getTime() < end2.getTime() ? end1 : end2;

        // if there is no overlap, return an empty array
        // this means that the 2 employees have worked on the same project, but not together
        if (largestStart.getTime() >= smallestEnd.getTime()) {
            return new String[] {};
        }

        // otherwise, return the overlap days as a string array
        // here we use format (SimpleDateFormat) instance and call its method .format() that will return a string from the Date
        // this will return [projectId, startDate, endDate, days worked together]
        int days = (int) (Math.abs(smallestEnd.getTime() - largestStart.getTime()) / (1000 * 60 * 60 * 24));
        return new String[] { projectId, format.format(largestStart), format.format(smallestEnd), Integer.toString(days) };
    }

    private static Map<String, Integer> calculatePairTotalDays(Map<String, List<String[]>> map) throws ParseException {
        // this method will return each pair of employees again, but as a value will have the total days worked together from all different projects
        // overlapping days from different projects will be counted only once
        Map<String, Integer> pairs_total_days = new HashMap<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        for(String key : map.keySet()) {
            List<String[]> list = new ArrayList<>();
            for(String[] arr : map.get(key)) {
                // save only the dates [start,end] of each project
                list.add(new String[] {arr[1], arr[2]});
            }
            int totalDaysWorked = 0;
            // this is the algorithm I came up with to calculate the total days - overallping days
            // it takes the first in the list project's startDate1 and endDate1 and adjust these values
            // during the while loop if it meets other projects in the list with overlapping dates
            // the projects that have overlapping dates get deleted from the list and the ones that don't have overlapping days
            // with the referenced project's startDate1 and endDate1 are skipped and left for the next iterations
            // in each iteration we add the days between the endDate1 - startDate1
            while(!list.isEmpty()) {
                Date startDate1 = format.parse(list.get(0)[0]);
                Date endDate1 = format.parse(list.get(0)[1]);
                list.remove(0);
                int index = 0;
                int size = list.size();
                while(size > 0) {
                    Date startDate2 = format.parse(list.get(index)[0]);
                    Date endDate2 = format.parse(list.get(index)[1]);
                    if(startDate2.getTime() <= startDate1.getTime() && (endDate2.getTime() < endDate1.getTime() && endDate2.getTime() > startDate1.getTime())) {
                        startDate1 = startDate2;
                        list.remove(index);
                    }
                    else if(startDate2.getTime() >= startDate1.getTime() && endDate2.getTime() <= endDate1.getTime()) {
                        list.remove(index);
                    }
                    else if(endDate2.getTime() > endDate1.getTime() && (startDate2.getTime() > startDate1.getTime() && startDate2.getTime() <= endDate1.getTime())) {
                        endDate1 = endDate2;
                        list.remove(index);
                    }
                    else if(startDate2.getTime() < startDate1.getTime() && endDate2.getTime() > endDate1.getTime()) {
                        startDate1 = startDate2;
                        endDate1 = endDate2;
                        list.remove(index);
                    }
                    else {
                        index++;
                    }
                    size--;
                }
                totalDaysWorked+= (int) (Math.abs(endDate1.getTime() - startDate1.getTime()) / (1000 * 60 * 60 * 24));
            }
            pairs_total_days.put(key, totalDaysWorked);
        }
        return pairs_total_days;
    }
}