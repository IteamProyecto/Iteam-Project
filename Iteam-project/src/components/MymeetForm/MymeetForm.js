import React, {Component, PropTypes} from 'react';
import axios from 'axios';
import {connect} from 'react-redux';
import {List, ListItem, ListDivider, ListSubHeader} from 'react-toolbox/lib/list';
import InputComponent from '../InputComponent/InputComponent'
import Dialog from 'react-toolbox/lib/dialog';
import TimePicker from 'react-toolbox/lib/time_picker';
import DatePicker from 'react-toolbox/lib/date_picker';
import {push} from 'react-router-redux';
import {PATHS} from '../../constants/routes';
import classes from './MymeetForm.scss';
import cssClasses from '../ComponentCSSForms/componentCSS.scss'
import BootstrapModal from '../../components/BootstrapModal/BootstrapModal';
import ListItem1 from './ListItem1.scss';
import ListItem2 from './ListItem2.scss';
import listFormat from './List.scss';
import chipTheme from './chips.scss';
import {updateMeetingId} from '../../redux/reducers/Meeting/MeetingReducer';
import {MEETING} from '../../constants/HostConfiguration';
import themeLabel from './label.scss';
import datesInput from './dateInput.scss'
import Dropdown from 'react-toolbox/lib/dropdown';
import Tooltip from 'react-toolbox/lib/tooltip';
import {Button} from 'react-toolbox/lib/button';
import Chip from 'react-toolbox/lib/chip';
import {saveConfig} from '../../redux/reducers/Meeting/MeetingConfigReducer';
import Spinner from '../Spinner/Spinner';
import {validateDate, validateStart, validateHour, changeEndDate} from '../../utils/DateUtils'
import dialogTheme from './dialog.scss'

var programDate = new Date();
var endDate = new Date();

const TooltipButton = Tooltip(Button);

const technics = [{value: 0, label: 'Brainstorming'}, {value: 1, label: 'SCAMPER'}, {
  value: 2,
  label: 'morphological analysis'
}];

const mapStateToProps = (state) => {
  if (state.loginUser !== null) {
    return {
      user: state.loginUser.user.username
    }
  }
};

const mapDispatchToProps = (dispatch) => ({

  onClick: () => dispatch(push('/' + PATHS.MENULOGGEDIN.PERSONALBOARD)),
  updateMyMeetingId: (meetingId) => dispatch(updateMeetingId(meetingId)),
  saveMeetingConfig: (meeting) => dispatch(saveConfig(meeting))
});


class MymeetForm extends Component {

  constructor(props) {
    super(props);
    this.state = {
      time: new Date(),
      owner: '',
      meetings: {},
      date: new Date(),
      active: false,
      config: {},
      meetEdit: {},
      editable: true,
      technicValue: '',
      teamName: {},
      tag: '',
      showSpinner: true,
      endTime: new Date()
    }
  }

  componentDidMount() {
    axios.get(MEETING.MEETING_BY_USER, {params: {username: this.props.user}}).then(function (response) {
      this.fillFields(response.data)
    }.bind(this));
  }

  adminActionsStart = [
    //FIXME: cancel button cannot have the same behave as the onclick in the "card meeting"
    {label: "Cancel", onClick: this.handleToggleDialog},
    {label: "Start", onClick: this.startMeeting.bind(this)}
  ];

  adminActionsEdit = [
    {label: "Cancel", onClick: this.handleToggleDialog},
    {label: "Delete MeetingConfig", onClick: this.handleToggleDialog},
    {label: "Edit", onClick: this.edit.bind(this)},
    {label: "Save", onClick: this.save.bind(this)}
  ];
  adminUserActionsFinish = [
    {label: "Cancel", onClick: this.handleToggleDialog},
    {label: "View Reports", onClick: this.goToReports.bind(this)}
  ];

  userActionsJoin = [
    {label: "Cancel", onClick: this.handleToggleDialog},
    {label: "Join", onClick: this.startMeeting.bind(this)}
  ];

  userActionsView = [
    {label: "OK", onClick: this.handleToggleDialog}
  ];


  goToReports() {

    let meetingInfo = {};
    meetingInfo.topic = this.state.meetEdit.topic;
    meetingInfo.owner = this.state.owner;
    meetingInfo.config = this.state.config;
    //Reducer containing toolbar info
    this.props.saveMeetingConfig(meetingInfo);

    //Reducer for meeting ID
    this.props.updateMyMeetingId(this.state.meetEdit.meetingId);
  }

  startMeeting() {
    //Object that contains meeting info for reducer for Toolbar
    let meetingInfo = {};
    meetingInfo.topic = this.state.meetEdit.topic;
    meetingInfo.owner = this.state.owner;
    meetingInfo.config = this.state.config;

    //Reducer containing toolbar info
    this.props.saveMeetingConfig(meetingInfo);

    //Reducer for meeting ID
    this.props.updateMyMeetingId(this.state.meetEdit.meetingId);

    //Dispatch to personal board
    this.props.onClick();
  }

  handleToggleDialog = (meeting) => {
    this.setState({
      active: !this.state.active,
      datetime: meeting.programmedDate,
      owner: meeting.ownerName,
      time: meeting.programmedDate,
      endTime: meeting.endDate,
      config: meeting.meetingConfig,
      meetEdit: meeting,
      editable: true
    });

    let datetime = new Date(meeting.programmedDate);
    programDate.setFullYear(datetime.getFullYear());
    programDate.setMonth(datetime.getMonth());
    programDate.setDate(datetime.getDate());
    programDate.setHours(datetime.getHours());
    programDate.setMinutes(datetime.getMinutes());

    let enddatetime = new Date(meeting.endDate);
    endDate.setFullYear(enddatetime.getFullYear());
    endDate.setMonth(enddatetime.getMonth());
    endDate.setDate(enddatetime.getDate());
    endDate.setHours(enddatetime.getHours());
    endDate.setMinutes(enddatetime.getMinutes());
  };

  isAdmin(owner) {
    return this.props.user === owner;
  }

  showActions(meetingOwner, meetingDate) {
    if (this.isAdmin(meetingOwner)) {
      //fecha ya paso, puede ver reportes
      if (validateDate(meetingDate)) {
        return this.adminUserActionsFinish;
      }
      else {
        //rango de tiempo aceptable para comenzar reunion
        if (validateStart(meetingDate)) {
          return this.adminActionsStart;
        }
        //fecha mayor, puede editar
        return this.adminActionsEdit;
      }
    }
    else {
      //fecha ya paso
      if (validateDate(meetingDate)) {
        return this.adminUserActionsFinish;
      }
      else {
        //rango de tiempo aceptable para unirse reunion
        if (validateStart(meetingDate)) {
          return this.userActionsJoin;
        }
        // fecha mayor, puede poner ver
        return this.userActionsView;
      }
    }
  }

  renderDate(meetingTime) {
    return new Date(meetingTime).toLocaleDateString([], {hour: '2-digit', minute: '2-digit'});
  }

  fillFields(meetings) {
    this.setState({meetings: meetings, showSpinner: false});
  }

  edit() {
    this.setState({editable: false})
  }

  save() {
    let editedMeeting = this.state.meetEdit;

    editedMeeting.meetingId = this.state.meetEdit.meetingId;
    editedMeeting.meetingConfig = this.state.config;

    axios.post(MEETING.MEETING_UPDATE, editedMeeting).then(
      function (response) {
      }
    ).catch(function (response) {
      //TODO: implement something here
    });

    this.setState({active: !this.state.active});
  }

  onChangeTopic = (topic) => {
    let newMeeting = this.state.meetEdit;
    newMeeting.topic = topic;
    this.setState({
      meetEdit: newMeeting
    });
  };

  onChangeVotes = (votes) => {
    let newMeeting = this.state.meetEdit;
    newMeeting.meetingConfig.votes = votes;
    this.setState({
      meetEdit: newMeeting
    });
  };

  onChangeSharedBoardTime = (sharedBoardTime) => {
    let newMeeting = this.state.meetEdit;
    newMeeting.meetingConfig.sharedBoardTime = sharedBoardTime;
    this.setState({
      meetEdit: newMeeting
    });
  };

  onChangePersonalBoardTime = (personalBoardTime) => {
    let newMeeting = this.state.meetEdit;
    newMeeting.meetingConfig.personalBoardTime = personalBoardTime;
    this.setState({
      meetEdit: newMeeting
    });
  };

  onChangeDescription = (description) => {
    let newMeeting = this.state.meetEdit;
    newMeeting.description = description;
    this.setState({
      meetEdit: newMeeting
    });
  };

  onChangeProgrammedDate = (date) => {
    let newDate = new Date(changeEndDate(programDate, endDate, date));

    programDate.setFullYear(date.getFullYear());
    programDate.setMonth(date.getMonth());
    programDate.setDate(date.getDate());

    endDate.setFullYear(newDate.getFullYear());
    endDate.setMonth(newDate.getMonth());
    endDate.setDate(newDate.getDate());

    let newMeeting = this.state.meetEdit;
    newMeeting.programmedDate = programDate.getTime();
    newMeeting.endDate = endDate.getTime();
    this.setState({
      meetEdit: newMeeting,
      datetime: date
    });
  };

  onChangeProgrammedTime = (time) => {
    if (validateHour(time)) {
      programDate.setHours(time.getHours());
      programDate.setMinutes(time.getMinutes());

      let newMeeting = this.state.meetEdit;
      newMeeting.programmedDate = programDate.getTime();
      this.setState({
        meetEdit: newMeeting,
        time: time
      });
    }
    else {
      this.setState({message: '¡You have to complete with valid time!'});
      this.refs.mymeetingModal.openModal();
    }
  };

  onChangeEndTime = (time) => {
    //TODO:check what happen here.
    endDate = new Date(checkDate(programDate, endDate, programDate.getHours(), time.getHours()));
    endDate.setHours(time.getHours());
    endDate.setMinutes(time.getMinutes());

    let newMeeting = this.state.meetEdit;
    newMeeting.endDate = endDate.getTime();
    this.setState({
      meetEdit: newMeeting,
      endTime: time
    });
  };

  handleChangeCombo = (value) => {
    let filteredLabelObject = technics.filter(filter => filter["value"] == value);
    let newConfig = this.state.config;

    newConfig.technic = value;

    this.setState({config: newConfig, technicValue: filteredLabelObject[0]["label"]})
  };


  handleChange = (name, value) => {
    this.setState({...this.state, [name]: value});
  };

  deleteTag(pos) {
    let newTags = this.state.config.tags;

    newTags.map(function (filter, index) {
      if (pos === index) {
        newTags.splice(index, 1);
      }
    });

    let newConfig = this.state.config;
    newConfig.tags = newTags;

    this.setState({config: newConfig, tag: ''});
  }

  handleAddTag() {
    if (this.state.tag !== '') {
      let newTags = this.state.config.tags;

      newTags.push((this.state.tag));

      let newConfig = this.state.config;

      newConfig.tags = newTags;
      this.setState({config: newConfig, tag: ''});
    }
  }

  tagLabels() {
    if (typeof this.state.config.tags !== "undefined") {
      return this.state.config.tags.map(function (tag, index) {
        return (
          <Chip key={index} deletable={!this.state.editable} onDeleteClick={this.deleteTag.bind(this, index)}
                theme={chipTheme}>
            {tag}
          </Chip>
        );
      }.bind(this));
    }
  }

  showDialog() {
    return this.isAdmin(this.state.meetEdit.ownerName) ? this.showDialogForAdmin() : this.showDialogForUser()
  }

  showDialogForUser() {
    return (
      <Dialog
        theme={dialogTheme}
        actions={this.showActions(this.state.meetEdit.ownerName, this.state.meetEdit.programmedDate)}
        active={this.state.active}
        onEscKeyDown={this.handleToggleDialog}
        onOverlayClick={this.handleToggleDialog}>
        <InputComponent className="col-md-8" label="Topic" value={this.state.meetEdit.topic} disable={false}/>
        <InputComponent className="col-md-8" label="Description" value={this.state.meetEdit.description}
                        disable={false}/>
        <DatePicker label='Date' sundayFirstDayOfWeek value={new Date(this.state.datetime)}
                    readonly={this.state.editable} onChange={this.onChangeProgrammedDate.bind(this)}
                    minDate={new Date()} theme={datesInput}/>
        <TimePicker label='Time'
                    value={isNaN(new Date(this.state.time)) ? 0 : new Date(this.state.time)}
                    readonly={this.state.editable} onChange={this.onChangeProgrammedTime.bind(this)}
                    theme={datesInput}/>
        <TimePicker label='End Time'
                    value={isNaN(new Date(this.state.endTime)) ? 0 : new Date(this.state.endTime)}
                    readonly={this.state.editable} onChange={this.onChangeEndTime.bind(this)}
                    theme={themeLabel}/>
      </Dialog>
    )
  }

  showDialogForAdmin() {
    return (
      <Dialog theme={dialogTheme}
        actions={this.showActions(this.state.meetEdit.ownerName, this.state.meetEdit.programmedDate)}
        active={this.state.active}
        onEscKeyDown={this.handleToggleDialog}
        onOverlayClick={this.handleToggleDialog}>

        <InputComponent label="Topic" value={this.state.meetEdit.topic} maxLength={30}
                        onValueChange={this.onChangeTopic.bind(this)}
                        disable={this.state.editable}/>

        <InputComponent label="Description" value={this.state.meetEdit.description} maxLength={144}
                        onValueChange={this.onChangeDescription.bind(this)} disable={this.state.editable}/>

        <div className="row col-md-12">
          <div className="col-md-4">
            <DatePicker label='Date' sundayFirstDayOfWeek value={new Date(this.state.datetime)}
                        readonly={this.state.editable} onChange={this.onChangeProgrammedDate.bind(this)}
                        minDate={new Date()}
                        theme={datesInput}/>
          </div>
          <div className="col-md-3">
            <TimePicker label='Start Time'
                        value={isNaN(new Date(this.state.time)) ? 0 : new Date(this.state.time)}
                        readonly={this.state.editable} onChange={this.onChangeProgrammedTime.bind(this)}
                        ttheme={datesInput}/>
          </div>
          <div className="col-md-3">
            <TimePicker label='End Time'
                        value={isNaN(new Date(this.state.endTime)) ? 0 : new Date(this.state.endTime)}
                        readonly={this.state.editable} onChange={this.onChangeEndTime.bind(this)}
                        theme={datesInput}/>
          </div>
        </div>
        <div className="row col-md-12">
          <InputComponent className="col-md-3" value={this.state.config.votes} disable={this.state.editable}
                          type="number" onValueChange={this.onChangeVotes.bind(this)} minValue={0} label="Votes"/>
          <InputComponent className="col-md-4" label="Minutes in personal board"
                          value={this.state.config.personalBoardTime}
                          onValueChange={this.onChangePersonalBoardTime.bind(this)} disable={this.state.editable}
                          minValue={0}/>
          <InputComponent className="col-md-4" label="Minutes in shared board" value={this.state.config.sharedBoardTime}
                          onValueChange={this.onChangeSharedBoardTime.bind(this)} minValue={0}/>
        </div>
        <div className="row col-md-12">
          <Dropdown label="Technic" auto onChange={this.handleChangeCombo.bind(this)}
                    source={technics} disabled={this.state.editable} value={this.state.config.technic}
                    theme={themeLabel}/>
        </div>
        <div className="row col-md-12">
          <InputComponent className="col-md-8" label="Tag" value={this.state.tag} disable={this.state.editable}
                          onValueChange={this.handleChange.bind(this)} maxLength={30}/>
          <div className="col-md-4">
            <TooltipButton icon='add' tooltip='Add tag' floating mini
                           disabled={this.state.editable} onClick={this.handleAddTag.bind(this)}/>
          </div>
        </div>
        {this.tagLabels()}
      </Dialog>
    )
  }

  render() {
    if (!this.state.showSpinner) {
      let meets = this.state.meetings;
      let meetingTime = new Date;

      let meetMap = [].slice.call(meets).sort(function (a, b) {
        return a.programmedDate - b.programmedDate
      });

      return (
        <div className={"container " + cssClasses.containerForm}>
          <div className={cssClasses.labelMainTitle}>
            <label>MY MEETINGS</label>
          </div>
          <BootstrapModal ref="mymeetingModal" message={this.state.message}/>
          <List theme={listFormat} selectable ripple>
            <ListSubHeader />
            {Object.keys(meetMap).map((key) => {
                meetingTime = meetMap[key].programmedDate;
                let renderDateTime = this.renderDate(meetingTime);
                let future_date = validateDate(meetingTime);
                let color = future_date ? ListItem2 : ListItem1;
                return (
                  <div key={key}>
                    <ListItem
                      theme={color}
                      caption={meetMap[key].topic}
                      legend={renderDateTime}
                      leftIcon='send'
                      rightIcon='visibility'
                      onClick={this.handleToggleDialog.bind(this, meetMap[key])}/>
                    <ListDivider />
                    <BootstrapModal ref="meetingModal" message={this.state.message}/>
                  </div>
                );
              }
            )}
            {this.showDialog()}
          </List>
        </div>
      )
    } else {
      return (
        <Spinner/>
      )
    }
  }
}

MymeetForm.propTypes = {
  onClick: PropTypes.func,
  user: PropTypes.any,
  goToReports: PropTypes.func,
  saveMeetingConfig: PropTypes.func
};

export default connect(mapStateToProps, mapDispatchToProps)(MymeetForm);
