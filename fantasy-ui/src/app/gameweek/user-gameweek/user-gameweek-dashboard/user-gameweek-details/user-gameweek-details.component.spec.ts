import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekDetailsComponent } from './user-gameweek-details.component';

describe('UserGameweekDetailsComponent', () => {
  let component: UserGameweekDetailsComponent;
  let fixture: ComponentFixture<UserGameweekDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
