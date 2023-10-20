# Wrong finacial data updated

## Issue

The user said the finacial data is change for year 2021:
![Issue of wrong data](pic/issueofwrongdata.png)

But in 2023, user can't modify year 2021's data:

![data 2021](pic/data2021.png)

User can only moidy year 2022's data:

![data 2022](pic/data2022.png)


## How to modify the data which can't be modified

The modify action ["Save" button trigger] will trigger post api request as below:

![Post data for 2022](pic/postData2022.png)

there could see the data for 2022 in updated.

And in year 2022, there is a button of "Save"  which is enabled, and set the network to "Slow 3G" and select "2021" year, before the UI refresh finish, click "Save" button.
Then the post body will have year "2021" and data of 2022 which is not refreshed 

![Post data for 2021](pic/postData2021.png)

## How to avoid this?

1. check in backend-- year 2021 should be blocked in api call 
2. don't trust froutend input -- front end status may be in corrupted format


