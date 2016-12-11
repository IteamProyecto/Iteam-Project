/**
 * Created by Randanne on 15/10/2016.
 */
import React from 'react'
import ReportsTestForm from '../../components/ReportsTestForm/D3ChartTree/PageChartTree.js'
import classes from './ReportsTestView.scss'

export class ReportsTestView extends React.Component {
  render() {
    return (
      <div className={classes.form}>
        <ReportsTestForm  />
      </div>
    );
  }
}

export default ReportsTestView
